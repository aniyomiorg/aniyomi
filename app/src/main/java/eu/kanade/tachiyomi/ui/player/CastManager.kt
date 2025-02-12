package eu.kanade.tachiyomi.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import eu.kanade.tachiyomi.ui.player.cast.CastMediaBuilder
import eu.kanade.tachiyomi.ui.player.cast.CastSessionListener
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.tail.TLMR
import java.util.LinkedList

class CastManager(
    private val context: Context,
    private val activity: PlayerActivity,
) {
    private val viewModel by activity.viewModels<PlayerViewModel> { PlayerViewModelProviderFactory(activity) }
    private val player by lazy { activity.player }
    private val playerPreferences: PlayerPreferences by lazy { viewModel.playerPreferences }
    private val autoplayEnabled = playerPreferences.autoplayEnabled().get()

    private val _castState = MutableStateFlow(CastState.DISCONNECTED)
    val castState: StateFlow<CastState> = _castState.asStateFlow()

    var castContext: CastContext? = null
        private set
    private var castSession: CastSession? = null
    private var sessionListener: CastSessionListener? = null
    private val mediaBuilder = CastMediaBuilder(viewModel, activity)
    private var castProgressJob: Job? = null

    private val isCastApiAvailable: Boolean
        get() = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    private val mediaQueue = LinkedList<MediaQueueItem>()
    private var isLoadingMedia = false

    init {
        initializeCast()
    }

    private fun initializeCast() {
        if (!isCastApiAvailable) return
        try {
            castContext = CastContext.getSharedInstance(context.applicationContext)
            sessionListener = CastSessionListener(this)
            registerSessionListener()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    // Session Management
    fun registerSessionListener() {
        sessionListener?.let { listener ->
            castContext?.sessionManager?.addSessionManagerListener(listener, CastSession::class.java)
        }
    }

    fun unregisterSessionListener() {
        sessionListener?.let { listener ->
            castContext?.sessionManager?.removeSessionManagerListener(listener, CastSession::class.java)
        }
    }

    fun refreshCastContext() {
        castSession = castContext?.sessionManager?.currentCastSession
        if (castSession?.isConnected == true) updateCastState(CastState.CONNECTED)
    }

    fun cleanup() {
        unregisterSessionListener()
        castSession = null
    }

    // Session Callbacks
    fun onSessionConnected(session: CastSession) {
        castSession = session
        updateCastState(CastState.CONNECTED)
        startTrackingCastProgress()
    }

    fun onSessionEnded() {
        castProgressJob?.cancel()
        val lastPosition = getCurrentCastPosition()
        if (lastPosition > 0) viewModel.updateCastProgress(lastPosition.toFloat() / 1000)
        castSession = null
        updateCastState(CastState.DISCONNECTED)
        viewModel.resumeFromCast()
    }

    fun updateCastState(state: CastState) {
        _castState.value = state
        if (state == CastState.CONNECTED) player.paused = true
        activity.invalidateOptionsMenu()
    }

    // Quality Selection
    fun handleQualitySelection() {
        viewModel.videoList.filter { it.isNotEmpty() }
            .onEach { videos ->
                val hasQueueItems = (castSession?.remoteMediaClient?.mediaQueue?.itemCount ?: 0) > 0
                val hasMultipleQualities = videos.size > 1

                when {
                    hasQueueItems || hasMultipleQualities -> activity.runOnUiThread { showQualitySelectionDialog() }
                    else -> loadRemoteMedia()
                }
            }
            .launchIn(viewModel.viewModelScope)
    }

    // Queue Management
    private fun getQueueItems(): List<MediaQueueItem> {
        return castSession?.remoteMediaClient?.mediaQueue?.let { queue ->
            (0 until queue.itemCount).mapNotNull { index ->
                queue.getItemAtIndex(index)
            }
        } ?: emptyList()
    }

    private fun removeQueueItem(itemId: Int) {
        castSession?.remoteMediaClient?.queueRemoveItem(itemId, null)
    }

    private fun moveQueueItem(itemId: Int, newIndex: Int) {
        castSession?.remoteMediaClient?.queueMoveItemToNewIndex(itemId, newIndex, null)
    }

    private fun clearQueue() {
        mediaQueue.clear() // Limpiar cola local
        castSession?.remoteMediaClient?.let { client ->
            client.stop()
            client.load(
                MediaLoadRequestData.Builder()
                    .setMediaInfo(mediaBuilder.buildEmptyMediaInfo())
                    .setAutoplay(false)
                    .build(),
            )
        }
    }

    // Dialogs
    private fun showQualitySelectionDialog() {
        activity.runOnUiThread {
            val currentQuality = viewModel.selectedVideoIndex.value
            val qualities = viewModel.videoList.value.mapIndexed { index, video ->
                val isSelected = index == currentQuality
                val qualityText = StringBuilder().apply {
                    append(video.quality)
                    if (isSelected) append(" ✓")
                }.toString()
                qualityText
            }.toTypedArray()

            AlertDialog.Builder(context)
                .setTitle(context.stringResource(TLMR.strings.title_cast_quality))
                .setItems(qualities) { dialog, which ->
                    viewModel.setVideoIndex(which)
                    dialog.dismiss()
                    loadRemoteMedia()
                }
                .setPositiveButton(context.stringResource(TLMR.strings.cast_queue_title)) { _, _ ->
                    showQueueManagementDialog()
                }
                .setNeutralButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun showQueueManagementDialog() {
        val queueItems = getQueueItems()
        if (queueItems.isEmpty()) return

        val displayItems = queueItems.mapIndexed { index, item ->
            val metadata = item.media?.metadata
            val title = metadata?.getString(MediaMetadata.KEY_TITLE) ?: "Unknown"
            val subtitle = metadata?.getString(MediaMetadata.KEY_SUBTITLE) ?: ""
            val position = index + 1
            "$position. $title - $subtitle"
        }.toTypedArray()

        activity.runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle(context.stringResource(TLMR.strings.cast_queue_title))
                .setItems(displayItems) { _, which ->
                    showQueueItemOptionsDialog(queueItems[which], which)
                }
                .setPositiveButton(context.stringResource(TLMR.strings.action_clear_queue)) { _, _ ->
                    showClearQueueConfirmationDialog()
                }
                .setNeutralButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun showQueueItemOptionsDialog(item: MediaQueueItem, currentPos: Int) {
        activity.runOnUiThread {
            val title = item.media?.metadata?.getString(MediaMetadata.KEY_TITLE) ?: "Unknown"
            val options = arrayOf(
                context.stringResource(TLMR.strings.cast_remove_from_queue),
                context.stringResource(TLMR.strings.move_to_top),
                context.stringResource(TLMR.strings.move_to_position),
            )

            AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showRemoveConfirmationDialog(item)
                        1 -> moveQueueItem(item.itemId, 0)
                        2 -> showMoveToPositionDialog(item, currentPos)
                    }
                }
                .setNeutralButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun showMoveToPositionDialog(item: MediaQueueItem, currentPos: Int) {
        val queueSize = castSession?.remoteMediaClient?.mediaQueue?.itemCount ?: return
        val positions = Array(queueSize) { index ->
            "${context.stringResource(TLMR.strings.position)} ${index + 1}"
        }

        activity.runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle(context.stringResource(TLMR.strings.select_position))
                .setSingleChoiceItems(positions, currentPos) { dialog, newPos ->
                    if (newPos != currentPos) {
                        moveQueueItem(item.itemId, newPos)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun showRemoveConfirmationDialog(item: MediaQueueItem) {
        activity.runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle(context.stringResource(TLMR.strings.remove_from_queue_confirmation))
                .setMessage(item.media?.metadata?.getString(MediaMetadata.KEY_TITLE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    removeQueueItem(item.itemId)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun showClearQueueConfirmationDialog() {
        activity.runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle(context.stringResource(TLMR.strings.clear_queue_confirmation))
                .setMessage(context.stringResource(TLMR.strings.clear_queue_confirmation_message))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    clearQueue()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    // Media Loading & Progress Tracking
    @SuppressLint("SuspiciousIndentation")
    private fun loadRemoteMedia() {
        if (!isCastApiAvailable || isLoadingMedia) return
        val remoteMediaClient = castSession?.remoteMediaClient ?: return

        activity.lifecycleScope.launch {
            try {
                isLoadingMedia = true
                val selectedIndex = viewModel.selectedVideoIndex.value
                val mediaInfo = mediaBuilder.buildMediaInfo(selectedIndex)
                val currentLocalPosition = (player.timePos ?: 0).toLong()

                if (remoteMediaClient.mediaQueue.itemCount > 0) {
                    // Optimización: Pre-construir QueueItem
                    val queueItem = MediaQueueItem.Builder(mediaInfo)
                        .setAutoplay(autoplayEnabled)
                        .build()

                    // Agregar a cola local
                    mediaQueue.add(queueItem)

                    // Optimizar carga en lotes
                    if (mediaQueue.size >= BATCH_SIZE) {
                        loadQueueBatch(remoteMediaClient)
                    } else {
                        // Cargar individual si no hay suficientes items
                        remoteMediaClient.queueAppendItem(queueItem, null)
                        showAddedToQueueToast()
                    }
                } else {
                    // Primera carga: Optimizar metadatos
                    remoteMediaClient.load(
                        MediaLoadRequestData.Builder()
                            .setMediaInfo(mediaInfo)
                            .setAutoplay(autoplayEnabled)
                            .setCurrentTime(currentLocalPosition * 1000)
                            .setCustomData(null) // Optimización: No enviar datos innecesarios
                            .build(),
                    )
                }
                _castState.value = CastState.CONNECTED
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                showLoadErrorToast()
            } finally {
                isLoadingMedia = false
            }
        }
    }

    private fun loadQueueBatch(remoteMediaClient: RemoteMediaClient) {
        val batchItems = mediaQueue.take(BATCH_SIZE).toTypedArray()
        mediaQueue.removeAll(batchItems.toSet())

        remoteMediaClient.queueInsertItems(batchItems, MediaQueueItem.INVALID_ITEM_ID, null)
        showAddedToQueueToast()
    }

    private fun showAddedToQueueToast() {
        activity.runOnUiThread {
            Toast.makeText(
                context,
                context.stringResource(TLMR.strings.cast_video_added_to_queue),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun showLoadErrorToast() {
        activity.runOnUiThread {
            Toast.makeText(
                context,
                context.stringResource(TLMR.strings.cast_error_loading),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun startTrackingCastProgress() {
        castProgressJob?.cancel()
        castProgressJob = activity.lifecycleScope.launch {
            while (castSession?.isConnected == true) {
                val currentPosition = getCurrentCastPosition()
                viewModel.updateCastProgress(currentPosition.toFloat() / 1000)
                delay(1000)
            }
        }
    }

    private fun getCurrentCastPosition(): Long {
        return castSession?.remoteMediaClient?.approximateStreamPosition ?: 0
    }

    fun maintainCastSessionBackground() {
        castSession?.remoteMediaClient?.let { client ->
            if (client.isPlaying) {
                client.pause()
            }
        }
    }

    companion object {
        private const val BATCH_SIZE = 5 // Tamaño óptimo para carga en lotes
    }

    enum class CastState {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
    }
}
