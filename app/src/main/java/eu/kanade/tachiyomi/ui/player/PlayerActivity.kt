package eu.kanade.tachiyomi.ui.player

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.widget.SeekBar
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.PlayerActivityBinding
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
import eu.kanade.tachiyomi.ui.base.activity.BaseThemedActivity.Companion.applyAppTheme
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.PickerDialog
import `is`.xyz.mpv.SpeedPickerDialog
import `is`.xyz.mpv.StateRestoreCallback
import `is`.xyz.mpv.Utils
import logcat.LogPriority
import nucleus.factory.RequiresPresenter
import uy.kohesive.injekt.injectLazy
import java.io.File

@RequiresPresenter(PlayerPresenter::class)
class PlayerActivity :
    BaseRxActivity<PlayerActivityBinding,
        PlayerPresenter>(),
    MPVLib.EventObserver {

    companion object {
        fun newIntent(context: Context, anime: Anime, episode: Episode): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra("anime", anime.id)
                putExtra("episode", episode.id)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    private val preferences: PreferencesHelper by injectLazy()

    private val player get() = binding.player

    private var userIsOperatingSeekbar = false

    // private var lockedUI = false
    private lateinit var mDetector: GestureDetectorCompat

    private val animationHandler = Handler(Looper.getMainLooper())

    // Fade out gesture text
    private val gestureTextRunnable = Runnable {
        binding.gestureTextView.visibility = View.GONE
    }

    private fun showGestureText() {
        animationHandler.removeCallbacks(gestureTextRunnable)
        binding.gestureTextView.visibility = View.VISIBLE

        animationHandler.postDelayed(gestureTextRunnable, 500L)
    }

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser) {
                return
            }
            player.timePos = progress
            updatePlaybackPos(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            userIsOperatingSeekbar = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            userIsOperatingSeekbar = false
        }
    }

    private var currentVideoList: List<Video>? = null

    private var playerViewMode: Int = preferences.getPlayerViewMode()

    private var playerIsDestroyed = true

    @SuppressLint("ClickableViewAccessibility")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        logcat { "bruh" }
        applyAppTheme(preferences)
        super.onCreate(savedInstanceState)

        binding = PlayerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setVisibilities()
        player.initialize(applicationContext.filesDir.path)
        MPVLib.setOptionString("keep-open", "always")
        player.addObserver(this)

        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        val width = dm.widthPixels.toFloat()
        val height = dm.heightPixels.toFloat()

        mDetector = GestureDetectorCompat(this, Gestures(this, width, height))
        // player.setOnClickListener { binding.controls.isVisible = !binding.controls.isVisible }
        player.setOnTouchListener { _, event ->
            mDetector.onTouchEvent(event)
        }
        binding.cycleAudioBtn.setOnLongClickListener { pickAudio(); true }
        binding.cycleSpeedBtn.setOnLongClickListener { pickSpeed(); true }
        binding.cycleSubsBtn.setOnLongClickListener { pickSub(); true }

        binding.playbackSeekbar.setOnSeekBarChangeListener(seekBarChangeListener)
        // player.playFile(currentVideoList!!.first().videoUrl!!.toString())

        binding.nextBtn.setOnClickListener { presenter.nextEpisode() }
        binding.prevBtn.setOnClickListener { presenter.previousEpisode() }

        if (presenter?.needsInit() == true) {
            val anime = intent.extras!!.getLong("anime", -1)
            val episode = intent.extras!!.getLong("episode", -1)
            if (anime == -1L || episode == -1L) {
                finish()
                return
            }
            presenter.init(anime, episode)
        }

        playerIsDestroyed = false
    }

    fun toggleControls() {
        binding.controls.isVisible = !binding.controls.isVisible
    }

    private fun pickAudio() = selectTrack("audio", { player.aid }, { player.aid = it })

    private fun pickSub() = selectTrack("sub", { player.sid }, { player.sid = it })

    private fun selectTrack(type: String, get: () -> Int, set: (Int) -> Unit) {
        val tracks = player.tracks.getValue(type)
        val selectedMpvId = get()
        val selectedIndex = tracks.indexOfFirst { it.mpvId == selectedMpvId }
        val restore = pauseForDialog()

        with(MaterialAlertDialogBuilder(this)) {
            setSingleChoiceItems(
                tracks.map { it.name }.toTypedArray(),
                selectedIndex
            ) { dialog, item ->
                val trackId = tracks[item].mpvId

                set(trackId)
                dialog.dismiss()
                trackSwitchNotification { TrackData(trackId, type) }
            }
            setOnDismissListener { restore() }
            create().show()
        }
    }

    private fun pauseForDialog(): StateRestoreCallback {
        val wasPlayerPaused = player.paused ?: true // default to not changing state
        player.paused = true
        return {
            if (!wasPlayerPaused) {
                player.paused = false
            }
        }
    }

    private fun pickSpeed() {
        // TODO: replace this with SliderPickerDialog
        val picker = SpeedPickerDialog()

        val restore = pauseForDialog()
        genericPickerDialog(picker, R.string.title_speed_dialog, "speed") {
            updateSpeedButton()
            restore()
        }
    }

    private fun genericPickerDialog(
        picker: PickerDialog,
        @StringRes titleRes: Int,
        property: String,
        restoreState: StateRestoreCallback
    ) {
        val dialog = with(AlertDialog.Builder(this)) {
            setTitle(titleRes)
            setView(picker.buildView(layoutInflater))
            setPositiveButton(R.string.dialog_ok) { _, _ ->
                picker.number?.let {
                    if (picker.isInteger()) {
                        MPVLib.setPropertyInt(property, it.toInt())
                    } else {
                        MPVLib.setPropertyDouble(property, it)
                    }
                }
            }
            setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
            setOnDismissListener { restoreState() }
            create()
        }

        picker.number = MPVLib.getPropertyDouble(property)
        dialog.show()
    }

    private fun setViewMode() {
        when (playerViewMode) {
            2 -> {
                MPVLib.setOptionString("video-aspect-override", "-1")
                MPVLib.setOptionString("panscan", "1.0")
            }
            1 -> {
                MPVLib.setOptionString("video-aspect-override", "-1")
                MPVLib.setOptionString("panscan", "0.0")
            }
            0 -> {
                val newAspect = "${binding.root.width}/${binding.root.height}"
                MPVLib.setOptionString("video-aspect-override", newAspect)
                MPVLib.setOptionString("panscan", "0.0")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setVisibilities() {
        // TODO: replace this atrocity
        binding.root.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && preferences.playerFullscreen()) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    fun updatePlaybackPos(position: Int) {
        binding.playbackPositionTxt.text = Utils.prettyTime(position)
        if (!userIsOperatingSeekbar) {
            binding.playbackSeekbar.progress = position
        }

        updateDecoderButton()
        updateSpeedButton()
    }

    private fun updatePlaybackDuration(duration: Int) {
        binding.playbackDurationTxt.text = Utils.prettyTime(duration)
        if (!userIsOperatingSeekbar) {
            binding.playbackSeekbar.max = duration
        }
    }

    private fun updateDecoderButton() {
        if (binding.cycleDecoderBtn.visibility != View.VISIBLE) {
            return
        }
        binding.cycleDecoderBtn.text = if (player.hwdecActive) "HW" else "SW"
    }

    private fun updateSpeedButton() {
        binding.cycleSpeedBtn.text = getString(R.string.ui_speed, player.playbackSpeed)
    }

    @Suppress("UNUSED_PARAMETER")
    fun playPause(view: View) = player.cyclePause()

    fun playPause() = player.cyclePause()

    fun doubleTapSeek(time: Int, event: MotionEvent) {
        val v = if (time < 0) binding.rewBg else binding.ffwdBg
        val x = event.x.toInt()
        val y = event.y.toInt()
        ViewAnimationUtils.createCircularReveal(v, x, y, 0f, kotlin.math.max(v.height, v.width).toFloat()).setDuration(300).start()

        ObjectAnimator.ofFloat(v, "alpha", 0f, 0.3f).setDuration(300).start()
        ObjectAnimator.ofFloat(v, "alpha", 0.3f, 0.3f, 0f).setDuration(600).start()
        // disable seeking when timePos is not available
        val duration = player.duration ?: 0
        val initialSeek = player.timePos ?: -1
        if (duration == 0) {
            return
        }
        val newPos = (initialSeek + time).coerceIn(0, duration)
        val newDiff = newPos - initialSeek
        // seek faster than assigning to timePos but less precise
        MPVLib.command(arrayOf("seek", newPos.toString(), "absolute+keyframes"))
        updatePlaybackPos(newPos)

        val diffText = Utils.prettyTime(newDiff, true)
        binding.gestureTextView.text = getString(R.string.ui_seek_distance, Utils.prettyTime(newPos), diffText)
        showGestureText()
    }

    data class TrackData(val track_id: Int, val track_type: String)

    private fun trackSwitchNotification(f: () -> TrackData) {
        val (track_id, track_type) = f()
        val trackPrefix = when (track_type) {
            "audio" -> getString(R.string.track_audio)
            "sub" -> getString(R.string.track_subs)
            "video" -> "Video"
            else -> "???"
        }

        if (track_id == -1) {
            toast("$trackPrefix ${getString(R.string.track_off)}")
            return
        }

        val trackName =
            player.tracks[track_type]?.firstOrNull { it.mpvId == track_id }?.name ?: "???"
        toast("$trackPrefix $trackName")
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleAudio(view: View) = trackSwitchNotification {
        player.cycleAudio(); TrackData(player.aid, "audio")
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleSub(view: View) = trackSwitchNotification {
        player.cycleSub(); TrackData(player.sid, "sub")
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleViewMode(view: View) {
        playerViewMode = when (playerViewMode) {
            0 -> 1
            1 -> 2
            2 -> 0
            else -> 1
        }
        preferences.setPlayerViewMode(playerViewMode)
        setViewMode()
    }

    private var currentQuality = 0

    @Suppress("UNUSED_PARAMETER")
    fun openSettings(view: View) {
        if (currentVideoList?.isNotEmpty() == true) {
            val qualityAlert = MaterialAlertDialogBuilder(this)

            qualityAlert.setTitle(R.string.playback_quality_dialog_title)

            var requestedQuality = 0
            val qualities = currentVideoList!!.map { it.quality }.toTypedArray()
            qualityAlert.setSingleChoiceItems(
                qualities,
                currentQuality
            ) { qualityDialog, selectedQuality ->
                if (selectedQuality > qualities.lastIndex) {
                    qualityDialog.cancel()
                } else {
                    requestedQuality = selectedQuality
                }
            }

            qualityAlert.setPositiveButton(android.R.string.ok) { qualityDialog, _ ->
                if (requestedQuality != currentQuality) {
                    currentQuality = requestedQuality
                    changeQuality(requestedQuality)
                }
                qualityDialog.dismiss()
            }

            qualityAlert.setNegativeButton(android.R.string.cancel) { qualityDialog, _ ->
                qualityDialog.cancel()
            }

            qualityAlert.show()
        }
    }

    private fun changeQuality(quality: Int) {
        setVideoList(null, quality, player.timePos)
    }

    @Suppress("UNUSED_PARAMETER")
    fun switchDecoder(view: View) {
        player.cycleHwdec()
        preferences.getPlayerViewMode()
        updateDecoderButton()
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleSpeed(view: View) {
        player.cycleSpeed()
        updateSpeedButton()
    }

    private fun refreshUi() {
        // forces update of entire UI, used when resuming the activity
        val paused = player.paused ?: return
        updatePlaybackStatus(paused)
        player.timePos?.let { updatePlaybackPos(it) }
        player.duration?.let { updatePlaybackDuration(it) }
        updatePlaylistButtons()
        updateEpisodeText()
        player.loadTracks()
    }

    private fun updateEpisodeText() {
        binding.fullTitleTextView.text = applicationContext.getString(
            R.string.playertitle,
            presenter.anime?.title,
            presenter.currentEpisode?.name
        )
    }

    private fun updatePlaylistButtons() {
        val plCount = presenter.episodeList.size
        val plPos = presenter.getCurrentEpisodeIndex()

        if (plCount == 1) {
            // use View.GONE so the buttons won't take up any space
            binding.prevBtn.visibility = View.GONE
            binding.nextBtn.visibility = View.GONE
            return
        }
        binding.prevBtn.visibility = View.VISIBLE
        binding.nextBtn.visibility = View.VISIBLE

        val g = ContextCompat.getColor(this, R.color.tint_disabled)
        val w = ContextCompat.getColor(this, R.color.tint_normal)
        binding.prevBtn.imageTintList = ColorStateList.valueOf(if (plPos == 0) g else w)
        binding.nextBtn.imageTintList = ColorStateList.valueOf(if (plPos == plCount - 1) g else w)
    }

    private fun updatePlaybackStatus(paused: Boolean) {
        val r = if (paused) R.drawable.ic_play_arrow_black_24dp else R.drawable.ic_pause_black_24dp
        binding.playBtn.setImageResource(r)

        if (paused) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDestroy() {
        presenter.deletePendingEpisodes()
        if (!playerIsDestroyed) {
            playerIsDestroyed = true
            player.destroy()
        }
        super.onDestroy()
    }

    override fun onStop() {
        presenter.saveEpisodeHistory()
        if (!playerIsDestroyed) {
            presenter.saveEpisodeProgress(player.timePos, player.duration)
            player.paused = true
        }
        super.onStop()
    }

    /**
     * Called from the presenter if the initial load couldn't load the videos of the episode. In
     * this case the activity is closed and a toast is shown to the user.
     */
    fun setInitialEpisodeError(error: Throwable) {
        logcat(LogPriority.ERROR, error)
        finish()
        toast(error.message)
    }

    fun setVideoList(videos: List<Video>?, videoPos: Int = 0, timePos: Int? = null) {
        if (playerIsDestroyed) return
        logcat(LogPriority.INFO) { "loaded!!" }
        currentVideoList = videos ?: currentVideoList
        currentVideoList?.getOrNull(videoPos)?.let {
            timePos?.let {
                MPVLib.command(arrayOf("set", "start", "$timePos"))
            } ?: presenter.currentEpisode?.last_second_seen?.let { pos ->
                val intPos = pos / 1000F
                MPVLib.command(arrayOf("set", "start", "$intPos"))
            }
            setViewMode()
            MPVLib.command(arrayOf("loadfile", it.videoUrl))
            it.subtitleTracks.forEachIndexed { i, sub ->
                val select = if (i == 0) "select" else "auto"
                MPVLib.command(arrayOf("sub-add", sub.url, select, sub.lang))
            }
            it.audioTracks.forEachIndexed { i, audio ->
                val select = if (i == 0) "select" else "auto"
                MPVLib.command(arrayOf("audio-add", audio.url, select, audio.lang))
            }
        }
        launchUI { refreshUi() }
    }

    fun setHttpOptions(headers: Map<String, String>) {
        val httpHeaderString = headers.map {
            it.key + ": " + it.value
        }.joinToString(",")
        MPVLib.setOptionString("http-header-fields", httpHeaderString)
        MPVLib.setOptionString("tls-verify", "no")
        MPVLib.setOptionString("cache-on-disk", "yes")
        val cacheDir = File(applicationContext.filesDir, "media").path
        MPVLib.setOptionString("cache-dir", cacheDir)
    }

    // mpv events

    private fun eventPropertyUi(property: String, value: Long) {
        when (property) {
            "time-pos" -> updatePlaybackPos(value.toInt())
            "duration" -> updatePlaybackDuration(value.toInt())
        }
    }

    private fun eventPropertyUi(property: String, value: Boolean) {
        when (property) {
            "pause" -> updatePlaybackStatus(value)
        }
    }

    override fun eventProperty(property: String) {}

    override fun eventProperty(property: String, value: Boolean) {
        runOnUiThread { eventPropertyUi(property, value) }
    }

    override fun eventProperty(property: String, value: Long) {
        runOnUiThread { eventPropertyUi(property, value) }
    }

    override fun eventProperty(property: String, value: String) {}

    override fun event(eventId: Int) {}
}
