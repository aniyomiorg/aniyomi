package eu.kanade.tachiyomi.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.activity.viewModels
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

    // Agregar nuevos StateFlows para Compose
    private val _queueItems = MutableStateFlow<List<MediaQueueItem>>(emptyList())
    val queueItems: StateFlow<List<MediaQueueItem>> = _queueItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    val availableDevices: StateFlow<List<CastDevice>> = _availableDevices.asStateFlow()

    // Nuevos estados para el reproductor
    private val _currentMedia = MutableStateFlow<CastMedia?>(null)
    val currentMedia: StateFlow<CastMedia?> = _currentMedia.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    data class CastMedia(
        val title: String,
        val subtitle: String,
        val thumbnail: String?,
    )

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
        updateCurrentMedia()

        // Actualizar cola inmediatamente al conectar
        updateQueueItems()

        session.remoteMediaClient?.registerCallback(
            object : RemoteMediaClient.Callback() {
                override fun onStatusUpdated() {
                    updateCurrentMedia()
                    updateQueueItems()
                }

                override fun onQueueStatusUpdated() {
                    updateQueueItems()
                }

                override fun onPreloadStatusUpdated() {
                    updateQueueItems()
                }
            },
        )
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

                if (!hasQueueItems && !hasMultipleQualities) {
                    loadRemoteMedia()
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

    fun removeQueueItem(itemId: Int) {
        castSession?.remoteMediaClient?.queueRemoveItem(itemId, null)
        updateQueueItems()
    }

    fun moveQueueItem(itemId: Int, newIndex: Int) {
        castSession?.remoteMediaClient?.queueMoveItemToNewIndex(itemId, newIndex, null)
        updateQueueItems()
    }

    fun clearQueue() {
        mediaQueue.clear()
        castSession?.remoteMediaClient?.let { client ->
            client.stop()
            client.load(
                MediaLoadRequestData.Builder()
                    .setMediaInfo(mediaBuilder.buildEmptyMediaInfo())
                    .setAutoplay(false)
                    .build(),
            )
        }
        updateQueueItems()
    }

    // Media Loading & Progress Tracking
    @SuppressLint("SuspiciousIndentation")
    fun loadRemoteMedia() {
        if (!isCastApiAvailable || isLoadingMedia) return
        val remoteMediaClient = castSession?.remoteMediaClient ?: return

        activity.lifecycleScope.launch {
            try {
                isLoadingMedia = true
                _isLoading.value = true
                val selectedIndex = viewModel.selectedVideoIndex.value
                val mediaInfo = mediaBuilder.buildMediaInfo(selectedIndex)
                val currentLocalPosition = (player.timePos ?: 0).toLong()

                // Actualizar cola antes y después de cargar nuevo medio
                updateQueueItems()
                if (remoteMediaClient.mediaQueue.itemCount > 0) {
                    val queueItem = MediaQueueItem.Builder(mediaInfo)
                        .setAutoplay(autoplayEnabled)
                        .build()

                    mediaQueue.add(queueItem)
                    remoteMediaClient.queueAppendItem(queueItem, null)
                    showAddedToQueueToast()
                } else {
                    remoteMediaClient.load(
                        MediaLoadRequestData.Builder()
                            .setMediaInfo(mediaInfo)
                            .setAutoplay(autoplayEnabled)
                            .setCurrentTime(currentLocalPosition * 1000)
                            .build(),
                    )
                }
                updateQueueItems() // Actualizar cola después de cargar
                _castState.value = CastState.CONNECTED
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                showLoadErrorToast()
            } finally {
                isLoadingMedia = false
                _isLoading.value = false
            }
        }
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
        castSession?.let { session ->
            if (session.isConnected) {
                // Mantener la sesión activa pero pausado
                session.remoteMediaClient?.pause()
                _isPlaying.value = false
            }
        }
    }

    private fun updateQueueItems() {
        _queueItems.value = castSession?.remoteMediaClient?.mediaQueue?.let { queue ->
            (0 until queue.itemCount).mapNotNull { index ->
                queue.getItemAtIndex(index)
            }
        } ?: emptyList()
    }

    // Agregar función para resetear el estado
    fun reset() {
        mediaQueue.clear()
        _queueItems.value = emptyList()
        _isLoading.value = false
        _castState.value = CastState.DISCONNECTED
        castProgressJob?.cancel()
        castSession = null
    }

    // Agregar función para reconectar
    fun reconnect() {
        if (!isCastApiAvailable) return
        try {
            castContext = CastContext.getSharedInstance(context.applicationContext)
            castSession = castContext?.sessionManager?.currentCastSession
            if (castSession?.isConnected == true) {
                // Reconectar y restaurar estado
                updateCastState(CastState.CONNECTED)
                startTrackingCastProgress()
                updateQueueItems()
                updateCurrentMedia()

                // Registrar callbacks nuevamente
                castSession?.remoteMediaClient?.registerCallback(
                    object : RemoteMediaClient.Callback() {
                        override fun onStatusUpdated() {
                            updateCurrentMedia()
                            updateQueueItems()
                        }

                        override fun onQueueStatusUpdated() {
                            updateQueueItems()
                        }

                        override fun onPreloadStatusUpdated() {
                            updateQueueItems()
                        }
                    },
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    // Corrección: Usar MediaRouter para buscar dispositivos
    private val mediaRouter by lazy {
        androidx.mediarouter.media.MediaRouter.getInstance(context)
    }

    fun startDeviceDiscovery() {
        if (!isCastApiAvailable) return

        try {
            castContext?.let { castContext ->
                // No cambiar el estado a CONNECTING si ya está conectado
                if (_castState.value != CastState.CONNECTED) {
                    _castState.value = CastState.CONNECTING
                }

                val currentSession = castContext.sessionManager?.currentCastSession
                val selector = androidx.mediarouter.media.MediaRouteSelector.Builder()
                    .addControlCategory(androidx.mediarouter.media.MediaControlIntent.CATEGORY_LIVE_VIDEO)
                    .addControlCategory(androidx.mediarouter.media.MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                    .build()

                val callback = object : androidx.mediarouter.media.MediaRouter.Callback() {
                    override fun onRouteAdded(
                        router: androidx.mediarouter.media.MediaRouter,
                        route: androidx.mediarouter.media.MediaRouter.RouteInfo,
                    ) {
                        updateDevicesList(currentSession)
                    }

                    override fun onRouteRemoved(
                        router: androidx.mediarouter.media.MediaRouter,
                        route: androidx.mediarouter.media.MediaRouter.RouteInfo,
                    ) {
                        updateDevicesList(currentSession)
                    }

                    override fun onRouteChanged(
                        router: androidx.mediarouter.media.MediaRouter,
                        route: androidx.mediarouter.media.MediaRouter.RouteInfo,
                    ) {
                        updateDevicesList(currentSession)
                    }
                }

                mediaRouter.addCallback(selector, callback)
                updateDevicesList(currentSession)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            // No cambiar el estado a DISCONNECTED si ya está conectado
            if (_castState.value != CastState.CONNECTED) {
                _castState.value = CastState.DISCONNECTED
            }
        }
    }

    private fun updateDevicesList(currentSession: CastSession?) {
        val devices = mediaRouter.routes.mapNotNull { route ->
            if (!route.isDefault) {
                CastDevice(
                    id = route.id,
                    name = route.name ?: "Unknown Device",
                    isConnected = route.id == currentSession?.castDevice?.deviceId,
                )
            } else {
                null
            }
        }

        _availableDevices.value = devices

        // Solo actualizar el estado si:
        // 1. Hay un dispositivo conectado y no estábamos en estado CONNECTED
        // 2. No hay dispositivos y no estábamos en estado DISCONNECTED
        if (devices.any { it.isConnected }) {
            if (_castState.value != CastState.CONNECTED) {
                _castState.value = CastState.CONNECTED
            }
        } else if (devices.isEmpty() && _castState.value != CastState.DISCONNECTED) {
            _castState.value = CastState.DISCONNECTED
        }
    }

    fun connectToDevice(deviceId: String) {
        try {
            val route = mediaRouter.routes.find { it.id == deviceId }
            if (route != null) {
                // Si el dispositivo ya está conectado, no hacer nada
                if (route.id == castSession?.castDevice?.deviceId) {
                    return
                }
                mediaRouter.selectRoute(route)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    // Agregar data class para dispositivos Cast
    data class CastDevice(
        val id: String,
        val name: String,
        val isConnected: Boolean = false,
    )

    fun playPause() {
        castSession?.remoteMediaClient?.let { client ->
            if (client.isPlaying) client.pause() else client.play()
            _isPlaying.value = !_isPlaying.value
        }
    }

    fun stop() {
        castSession?.remoteMediaClient?.stop()
        _isPlaying.value = false
    }

    fun setVolume(volume: Float) {
        try {
            castSession?.let { session ->
                // El volumen de Cast va de 0.0 a 1.0
                val newVolume = volume.coerceIn(0f, 1f)
                session.volume = newVolume.toDouble()
                _volume.value = newVolume
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    private fun updateCurrentMedia() {
        castSession?.remoteMediaClient?.let { client ->
            val mediaInfo = client.mediaInfo
            val metadata = mediaInfo?.metadata

            _currentMedia.value = CastMedia(
                title = metadata?.getString(MediaMetadata.KEY_TITLE)
                    ?: viewModel.mediaTitle.value,
                subtitle = metadata?.getString(MediaMetadata.KEY_SUBTITLE)
                    ?: viewModel.currentEpisode.value?.name ?: "",
                thumbnail = metadata?.images?.firstOrNull()?.url?.toString()
                    ?: viewModel.currentAnime.value?.thumbnailUrl,
            )
            _isPlaying.value = !client.isPaused
            _volume.value = castSession?.volume?.toFloat() ?: 1f
        }
    }

    fun seekRelative(offset: Int) {
        castSession?.remoteMediaClient?.let { client ->
            val newPosition = client.approximateStreamPosition + (offset * 1000)
            client.seek(newPosition)
        }
    }

    fun nextVideo() {
        castSession?.remoteMediaClient?.let { client ->
            val queue = client.mediaQueue
            val currentItemId = client.currentItem?.itemId ?: return@let
            val currentIndex = (0 until queue.itemCount).find {
                queue.getItemAtIndex(it)?.itemId == currentItemId
            } ?: return@let

            if (currentIndex < queue.itemCount - 1) {
                client.queueJumpToItem(queue.getItemAtIndex(currentIndex + 1)?.itemId ?: return@let, null)
            }
        }
    }

    fun previousVideo() {
        castSession?.remoteMediaClient?.let { client ->
            val queue = client.mediaQueue
            val currentItemId = client.currentItem?.itemId ?: return@let
            val currentIndex = (0 until queue.itemCount).find {
                queue.getItemAtIndex(it)?.itemId == currentItemId
            } ?: return@let

            if (currentIndex > 0) {
                client.queueJumpToItem(queue.getItemAtIndex(currentIndex - 1)?.itemId ?: return@let, null)
            }
        }
    }

    fun endSession() {
        val mSessionManager = castContext!!.sessionManager
        mSessionManager.endCurrentSession(true)
        reset() // Limpia el estado actual
        _castState.value = CastState.DISCONNECTED
    }

    companion object {
        private const val BATCH_SIZE = 5
    }

    enum class CastState {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
    }
}
