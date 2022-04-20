package eu.kanade.tachiyomi.ui.player

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.util.Rational
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.databinding.PlayerActivityBinding
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import logcat.LogPriority
import nucleus.factory.RequiresPresenter
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

@RequiresPresenter(PlayerPresenter::class)
class PlayerActivity :
    BaseRxActivity<PlayerPresenter>(),
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

    override fun onNewIntent(intent: Intent) {
        val anime = intent.extras!!.getLong("anime", -1)
        val episode = intent.extras!!.getLong("episode", -1)
        if (anime == -1L || episode == -1L) {
            finish()
            return
        }
        presenter.saveEpisodeProgress(player.timePos, player.duration)
        presenter.saveEpisodeHistory()

        presenter.anime = null
        presenter.init(anime, episode)
        super.onNewIntent(intent)
    }

    private var isInPipMode: Boolean = false

    private var mReceiver: BroadcastReceiver? = null

    lateinit var binding: PlayerActivityBinding

    private val langName = LocaleHelper.getSimpleLocaleDisplay(preferences.lang().get())

    internal val player get() = binding.player

    private var audioManager: AudioManager? = null
    private var fineVolume = 0F
    private var maxVolume = 0

    private var brightness = 0F

    private var width = 0
    private var height = 0

    internal var isLocked = false

    private val windowInsetsController by lazy { WindowInsetsControllerCompat(window, binding.root) }

    private var audioFocusRestore: () -> Unit = {}

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { type ->
        when (type) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            -> {
                // loss can occur in addition to ducking, so remember the old callback
                val oldRestore = audioFocusRestore
                val wasPlayerPaused = player.paused ?: false
                player.paused = true
                audioFocusRestore = {
                    oldRestore()
                    if (!wasPlayerPaused) player.paused = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                MPVLib.command(arrayOf("multiply", "volume", 0.5F.toString()))
                audioFocusRestore = {
                    MPVLib.command(arrayOf("multiply", "volume", 2F.toString()))
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioFocusRestore()
                audioFocusRestore = {}
            }
        }
    }

    private var hasAudioFocus = false

    private val audioFocusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
    } else null

    @Suppress("DEPRECATION")
    private fun requestAudioFocus() {
        if (hasAudioFocus) return
        hasAudioFocus = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager!!.requestAudioFocus(audioFocusRequest!!)
        } else {
            audioManager!!.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        hasAudioFocus = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)
        } else {
            audioManager!!.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun setAudioFocus(paused: Boolean) {
        if (paused) {
            abandonAudioFocus()
        } else {
            requestAudioFocus()
        }
    }

    private var initialSeek = -1

    private lateinit var mDetector: GestureDetectorCompat

    private val animationHandler = Handler(Looper.getMainLooper())

    // Fade out seek text
    private val seekTextRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.fade_out_medium).also { fadeAnimation ->
            binding.seekView.startAnimation(fadeAnimation)
            binding.seekView.visibility = View.GONE
        }
    }

    // Fade out Volume Bar
    private val volumeViewRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.fade_out_medium).also { fadeAnimation ->
            binding.volumeView.startAnimation(fadeAnimation)
            binding.volumeView.visibility = View.GONE
        }
    }

    // Fade out Brightness Bar
    private val brightnessViewRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.fade_out_medium).also { fadeAnimation ->
            binding.brightnessView.startAnimation(fadeAnimation)
            binding.brightnessView.visibility = View.GONE
        }
    }

    private fun showGestureView(type: String) {
        val callback: Runnable
        val itemView: LinearLayout
        val delay: Long
        when (type) {
            "seek" -> {
                callback = seekTextRunnable
                itemView = binding.seekView
                delay = 500L
            }
            "volume" -> {
                callback = volumeViewRunnable
                itemView = binding.volumeView
                delay = 500L
            }
            "brightness" -> {
                callback = brightnessViewRunnable
                itemView = binding.brightnessView
                delay = 500L
            }
            else -> return
        }

        animationHandler.removeCallbacks(callback)
        itemView.visibility = View.VISIBLE
        animationHandler.postDelayed(callback, delay)
    }

    private var currentVideoList: List<Video>? = null

    private var playerViewMode: Int = preferences.getPlayerViewMode()

    private var playerIsDestroyed = true

    internal var subTracks: Array<Track> = emptyArray()

    internal var selectedSub = 0

    private var hadPreviousSubs = false

    internal var audioTracks: Array<Track> = emptyArray()

    internal var selectedAudio = 0

    private var hadPreviousAudio = false

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        binding.playerControls.resetControlsFade()
        return super.dispatchTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        registerSecureActivity(this)
        Utils.copyAssets(this)
        super.onCreate(savedInstanceState)

        binding = PlayerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = 70000000
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.navigationBarColor = 70000000
        }

        setVisibilities()
        binding.playerControls.showAndFadeControls()

        player.initialize(applicationContext.filesDir.path)
        MPVLib.setOptionString("keep-open", "always")
        player.addObserver(this)

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            launchUI { toast(throwable.message) }
            logcat(LogPriority.ERROR, throwable)
            finish()
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        fineVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        brightness = Utils.getScreenBrightness(this) ?: 0.5F

        volumeControlStream = AudioManager.STREAM_MUSIC

        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        width = dm.widthPixels
        height = dm.heightPixels

        val gestures = Gestures(this, width.toFloat(), height.toFloat())
        mDetector = GestureDetectorCompat(this, gestures)
        player.setOnTouchListener { v, event ->
            gestures.onTouch(v, event)
            mDetector.onTouchEvent(event)
        }

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

    /**
     * Switches to the previous episode if [previous] is true,
     * to the next episode if [previous] is false
     */
    internal fun switchEpisode(previous: Boolean) {
        val switchMethod = if (previous) presenter::previousEpisode else presenter::nextEpisode
        val errorRes = if (previous) R.string.no_previous_episode else R.string.no_next_episode

        presenter.saveEpisodeProgress(player.timePos, player.duration)
        presenter.saveEpisodeHistory()
        val wasPlayerPaused = player.paused
        player.paused = true
        showLoadingIndicator(true)

        val epTxt = switchMethod {
            if (wasPlayerPaused == false) {
                player.paused = false
            }
        }

        when {
            epTxt == "Invalid" -> return
            epTxt == null -> { launchUI { toast(errorRes) }; showLoadingIndicator(false) }
            isInPipMode -> launchUI { toast(epTxt) }
        }
    }

    fun toggleControls() = binding.playerControls.toggleControls()

    private fun showLoadingIndicator(visible: Boolean) {
        if (binding.loadingIndicator.isVisible == visible) return
        binding.playerControls.binding.playBtn.isVisible = !visible
        binding.loadingIndicator.isVisible = visible
    }

    internal fun setSub(index: Int) {
        if (selectedSub == index || selectedSub > subTracks.lastIndex) return
        selectedSub = index
        if (index == 0) {
            player.sid = -1
            return
        }
        val tracks = player.tracks.getValue("sub")
        val selectedLoadedTrack = tracks.firstOrNull {
            it.name == subTracks[index].url ||
                it.mpvId.toString() == subTracks[index].url
        }
        selectedLoadedTrack?.let { player.sid = it.mpvId }
            ?: MPVLib.command(arrayOf("sub-add", subTracks[index].url, "select", subTracks[index].url))
    }

    internal fun setAudio(index: Int) {
        if (selectedAudio == index || selectedAudio > audioTracks.lastIndex) return
        selectedAudio = index
        if (index == 0) {
            player.aid = -1
            return
        }
        val tracks = player.tracks.getValue("audio")
        val selectedLoadedTrack = tracks.firstOrNull {
            it.name == audioTracks[index].url ||
                it.mpvId.toString() == audioTracks[index].url
        }
        selectedLoadedTrack?.let { player.aid = it.mpvId }
            ?: MPVLib.command(arrayOf("audio-add", audioTracks[index].url, "select", audioTracks[index].url))
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
        binding.root.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LOW_PROFILE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && preferences.playerFullscreen()) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @SuppressLint("SourceLockedOrientationActivity")
    fun rotatePlayer(view: View) {
        if (this.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT) {
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            binding.playerControls.binding.controlsTopLandscape.isVisible = true
            binding.playerControls.binding.controlsTopPortrait.isVisible = false
        } else {
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            binding.playerControls.binding.controlsTopLandscape.isVisible = false
            binding.playerControls.binding.controlsTopPortrait.isVisible = true
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun playPause(view: View) {
        player.cyclePause()
        binding.playerControls.playPause()
    }

    private val doubleTapPlayPauseRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.fade_out_medium).also { fadeAnimation ->
            binding.playPauseView.startAnimation(fadeAnimation)
            binding.playPauseView.visibility = View.GONE
        }
    }

    fun doubleTapPlayPause() {
        animationHandler.removeCallbacks(doubleTapPlayPauseRunnable)
        playPause(binding.playerControls.binding.playBtn)

        if (!binding.playerControls.binding.controlsView.isVisible) {
            when {
                player.paused!! -> { binding.playPauseView.setImageResource(R.drawable.ic_pause_80dp) }
                !player.paused!! -> { binding.playPauseView.setImageResource(R.drawable.ic_play_arrow_80dp) }
            }

            AnimationUtils.loadAnimation(this, R.anim.fade_in_medium).also { fadeAnimation ->
                binding.playPauseView.startAnimation(fadeAnimation)
                binding.playPauseView.visibility = View.VISIBLE
            }

            animationHandler.postDelayed(doubleTapPlayPauseRunnable, 500L)
        } else binding.playPauseView.visibility = View.GONE
    }

    fun doubleTapSeek(time: Int, event: MotionEvent? = null) {
        val v = if (time < 0) binding.rewBg else binding.ffwdBg
        val w = if (time < 0) width * 0.2F else width * 0.8F
        val x = (event?.x?.toInt() ?: w.toInt()) - v.x.toInt()
        val y = (event?.y?.toInt() ?: height / 2) - v.y.toInt()
        ViewAnimationUtils.createCircularReveal(v, x, y, 0f, kotlin.math.max(v.height, v.width).toFloat()).setDuration(500).start()

        ObjectAnimator.ofFloat(v, "alpha", 0f, 0.2f).setDuration(500).start()
        ObjectAnimator.ofFloat(v, "alpha", 0.2f, 0.2f, 0f).setDuration(1000).start()
        val newPos = (player.timePos ?: 0) + time // only for display
        MPVLib.command(arrayOf("seek", time.toString(), "relative"))

        val diffText = Utils.prettyTime(time, true)
        binding.seekText.text = getString(R.string.ui_seek_distance, Utils.prettyTime(newPos), diffText)
        showGestureView("seek")
    }

    fun verticalScrollLeft(diff: Float) {
        brightness = (brightness + diff).coerceIn(-0.75F, 1F)
        window.attributes = window.attributes.apply {
            // value of 0 and 0.01 is broken somehow
            screenBrightness = brightness.coerceAtLeast(0.02F)
        }

        if (brightness < 0) {
            binding.brightnessOverlay.isVisible = true
            val alpha = (abs(brightness) * 256).toInt()
            binding.brightnessOverlay.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
        } else {
            binding.brightnessOverlay.isVisible = false
        }
        val finalBrightness = (brightness * 100).roundToInt()
        binding.brightnessText.text = finalBrightness.toString()
        binding.brightnessBar.progress = finalBrightness
        binding.brightnessBar.secondaryProgress = abs(finalBrightness)
        if (finalBrightness >= 0) { binding.brightnessImg.setImageResource(R.drawable.ic_brightness_positive_24dp); binding.brightnessBar.max = 100 } else { binding.brightnessImg.setImageResource(R.drawable.ic_brightness_negative_24dp); binding.brightnessBar.max = 75 }
        showGestureView("brightness")
    }

    fun verticalScrollRight(diff: Float) {
        fineVolume = (fineVolume + (diff * maxVolume)).coerceIn(0F, maxVolume.toFloat())
        val newVolume = fineVolume.toInt()
        // val newVolumePercent = 100 * newVolume / maxVolume
        audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)

        binding.volumeText.text = newVolume.toString()
        binding.volumeBar.progress = newVolume
        if (newVolume == 0) binding.volumeImg.setImageResource(R.drawable.ic_volume_none_24dp)
        else binding.volumeImg.setImageResource(R.drawable.ic_volume_high_24dp)
        showGestureView("volume")
    }

    fun initSeek() {
        initialSeek = player.timePos ?: -1
    }

    fun horizontalScroll(diff: Float) {
        // disable seeking when timePos is not available
        val duration = player.duration ?: 0
        if (duration == 0 || initialSeek < 0) {
            return
        }
        val newPos = (initialSeek + diff.toInt()).coerceIn(0, duration)
        val newDiff = newPos - initialSeek
        // seek faster than assigning to timePos but less precise
        MPVLib.command(arrayOf("seek", newPos.toString(), "absolute+keyframes"))
        binding.playerControls.updatePlaybackPos(newPos)

        val diffText = Utils.prettyTime(newDiff, true)
        binding.seekText.text = getString(R.string.ui_seek_distance, Utils.prettyTime(newPos), diffText)
        showGestureView("seek")
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleAudio(view: View) {
        if (audioTracks.isEmpty()) return
        setAudio(if (selectedAudio < audioTracks.lastIndex) selectedAudio + 1 else 0)
        toast("Audio: ${audioTracks[selectedAudio].lang}")
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleSub(view: View) {
        if (subTracks.isEmpty()) return
        setSub(if (selectedSub < subTracks.lastIndex) selectedSub + 1 else 0)
        toast("Sub: ${subTracks[selectedSub].lang}")
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleViewMode(view: View) {
        playerViewMode = when (playerViewMode) {
            0 -> 1
            1 -> 2
            2 -> 0
            else -> 0
        }
        preferences.setPlayerViewMode(playerViewMode)
        setViewMode()
    }

    private var currentQuality = 0

    @Suppress("UNUSED_PARAMETER")
    fun openQuality(view: View) {
        if (currentVideoList?.isNotEmpty() != true) return
        val qualityAlert = MaterialAlertDialogBuilder(this)

        qualityAlert.setTitle(R.string.playback_quality_dialog_title)

        var requestedQuality = 0
        val qualities = currentVideoList!!.map { it.quality }.toTypedArray()
        qualityAlert.setSingleChoiceItems(
            qualities,
            currentQuality,
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

    private fun changeQuality(quality: Int) {
        if (playerIsDestroyed) return
        logcat(LogPriority.INFO) { "changing quality" }
        currentVideoList?.getOrNull(quality)?.let {
            setHttpOptions(it)
            player.timePos?.let {
                MPVLib.command(arrayOf("set", "start", "${player.timePos}"))
            }
            subTracks = arrayOf(Track("nothing", "Off")) + it.subtitleTracks.toTypedArray()
            audioTracks = arrayOf(Track("nothing", "Off")) + it.audioTracks.toTypedArray()
            MPVLib.command(arrayOf("loadfile", parseVideoUrl(it.videoUrl)))
        }
        launchUI { refreshUi() }
    }

    @Suppress("UNUSED_PARAMETER")
    fun switchDecoder(view: View) {
        player.cycleHwdec()
        preferences.getPlayerViewMode()
        binding.playerControls.updateDecoderButton()
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleSpeed(view: View) {
        player.cycleSpeed()
        binding.playerControls.updateSpeedButton()
    }

    @Suppress("UNUSED_PARAMETER")
    fun skipIntro(view: View) {
        doubleTapSeek(85)
    }

    private fun refreshUi() {
        // forces update of entire UI, used when resuming the activity
        val paused = player.paused ?: return
        updatePlaybackStatus(paused)
        player.timePos?.let { binding.playerControls.updatePlaybackPos(it) }
        player.duration?.let { binding.playerControls.updatePlaybackDuration(it) }
        updatePlaylistButtons()
        updateEpisodeText()
        player.loadTracks()
    }

    private fun updateEpisodeText() {
        binding.playerControls.binding.titleMainTxtLandscape.text = presenter.anime?.title
        binding.playerControls.binding.titleSecondaryTxtLandscape.text = presenter.currentEpisode?.name
        binding.playerControls.binding.titleMainTxtPortrait.text = presenter.anime?.title
        binding.playerControls.binding.titleSecondaryTxtPortrait.text = presenter.currentEpisode?.name
    }

    private fun updatePlaylistButtons() {
        val plCount = presenter.episodeList.size
        val plPos = presenter.getCurrentEpisodeIndex()

        val grey = ContextCompat.getColor(this, R.color.tint_disabled)
        val white = ContextCompat.getColor(this, R.color.tint_normal)
        with(binding.playerControls.binding.prevBtn) {
            this.imageTintList = ColorStateList.valueOf(if (plPos == 0) grey else white)
            this.isClickable = plPos != 0
        }
        with(binding.playerControls.binding.nextBtn) {
            this.imageTintList = ColorStateList.valueOf(if (plPos == plCount - 1) grey else white)
            this.isClickable = plPos != plCount - 1
        }
    }

    private fun updatePlaybackStatus(paused: Boolean) {
        val r = if (paused) R.drawable.ic_play_arrow_80dp else R.drawable.ic_pause_80dp
        binding.playerControls.binding.playBtn.setImageResource(r)

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
            player.removeObserver(this)
            player.destroy()
        }
        abandonAudioFocus()
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            finishAndRemoveTask()
        }
        super.onDestroy()
    }

    override fun onStop() {
        presenter.saveEpisodeHistory()
        if (!playerIsDestroyed) {
            presenter.saveEpisodeProgress(player.timePos, player.duration)
            player.paused = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPipMode) finishAndRemoveTask()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        setVisibilities()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) player.paused?.let { updatePictureInPictureActions(!it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        isInPipMode = isInPictureInPictureMode
        binding.playerControls.hideControls(!isInPictureInPictureMode)
        if (isInPictureInPictureMode) binding.loadingIndicator.indicatorSize = binding.loadingIndicator.indicatorSize / 2
        else binding.loadingIndicator.indicatorSize = binding.loadingIndicator.indicatorSize * 2
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        if (isInPictureInPictureMode) {
            // On Android TV it is required to hide controller in this PIP change callback
            binding.playerControls.hideControls(true)
            mReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent == null || ACTION_MEDIA_CONTROL != intent.action) {
                        return
                    }
                    when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                        CONTROL_TYPE_PLAY -> {
                            player.paused = false
                            updatePictureInPictureActions(true)
                        }
                        CONTROL_TYPE_PAUSE -> {
                            player.paused = true
                            updatePictureInPictureActions(false)
                        }
                        CONTROL_TYPE_PREVIOUS -> {
                            switchEpisode(true)
                            player.paused?.let { updatePictureInPictureActions(!it) }
                        }
                        CONTROL_TYPE_NEXT -> {
                            switchEpisode(false)
                            player.paused?.let { updatePictureInPictureActions(!it) }
                        }
                    }
                }
            }
            registerReceiver(mReceiver, IntentFilter(ACTION_MEDIA_CONTROL))
        } else {
            if (mReceiver != null) {
                unregisterReceiver(mReceiver)
                mReceiver = null
            }
            binding.playerControls.hideControls(false)
        }
    }

    @Suppress("DEPRECATION")
    internal fun startPiP() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.playerControls.hideControls(true)
            player.paused?.let { updatePictureInPictureActions(!it) }
                ?.let { this.enterPictureInPictureMode(it) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteAction(
        iconResId: Int,
        titleResId: Int,
        requestCode: Int,
        controlType: Int,
    ): RemoteAction {
        return RemoteAction(
            Icon.createWithResource(this, iconResId),
            getString(titleResId),
            getString(titleResId),
            PendingIntent.getBroadcast(
                this,
                requestCode,
                Intent(ACTION_MEDIA_CONTROL)
                    .putExtra(EXTRA_CONTROL_TYPE, controlType),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePictureInPictureActions(
        playing: Boolean,
    ): PictureInPictureParams {
        val mPictureInPictureParams = PictureInPictureParams.Builder()
            // Set action items for the picture-in-picture mode. These are the only custom controls
            // available during the picture-in-picture mode.
            .setActions(
                arrayListOf(

                    createRemoteAction(
                        R.drawable.ic_skip_previous_24dp,
                        R.string.action_previous_episode,
                        CONTROL_TYPE_PREVIOUS,
                        REQUEST_PREVIOUS,
                    ),

                    if (playing) {
                        createRemoteAction(
                            R.drawable.ic_pause_24dp,
                            R.string.action_pause,
                            CONTROL_TYPE_PAUSE,
                            REQUEST_PAUSE,
                        )
                    } else {
                        createRemoteAction(
                            R.drawable.ic_play_arrow_24dp,
                            R.string.action_play,
                            CONTROL_TYPE_PLAY,
                            REQUEST_PLAY,
                        )
                    },
                    createRemoteAction(
                        R.drawable.ic_skip_next_24dp,
                        R.string.action_next_episode,
                        CONTROL_TYPE_NEXT,
                        REQUEST_NEXT,
                    ),

                ),
            )
            .setAspectRatio(player.videoAspect?.times(10000)?.let { Rational(it.toInt(), 10000) })
            .build()
        setPictureInPictureParams(mPictureInPictureParams)
        return mPictureInPictureParams
    }

    /**
     * Called from the presenter if the initial load couldn't load the videos of the episode. In
     * this case the activity is closed and a toast is shown to the user.
     */
    fun setInitialEpisodeError(error: Throwable) {
        launchUI { toast(error.message) }
        logcat(LogPriority.ERROR, error)
        finish()
    }

    fun setVideoList(videos: List<Video>) {
        if (playerIsDestroyed) return
        currentVideoList = videos
        currentVideoList?.firstOrNull()?.let {
            setHttpOptions(it)
            presenter.currentEpisode?.last_second_seen?.let { pos ->
                val intPos = pos / 1000F
                MPVLib.command(arrayOf("set", "start", "$intPos"))
            }
            setViewMode()
            subTracks = arrayOf(Track("nothing", "Off")) + it.subtitleTracks.toTypedArray()
            audioTracks = arrayOf(Track("nothing", "Off")) + it.audioTracks.toTypedArray()
            MPVLib.command(arrayOf("loadfile", parseVideoUrl(it.videoUrl)))
        }
        launchUI { refreshUi() }
    }

    private fun parseVideoUrl(videoUrl: String?): String? {
        val uri = Uri.parse(videoUrl)
        return openContentFd(uri) ?: videoUrl
    }

    private fun openContentFd(uri: Uri): String? {
        if (uri.scheme != "content") return null
        val resolver = applicationContext.contentResolver
        logcat { "Resolving content URI: $uri" }
        val fd = try {
            val desc = resolver.openFileDescriptor(uri, "r")
            desc!!.detachFd()
        } catch (e: Exception) {
            logcat { "Failed to open content fd: $e" }
            return null
        }
        // Find out real file path and see if we can read it directly
        try {
            val path = File("/proc/self/fd/$fd").canonicalPath
            if (!path.startsWith("/proc") && File(path).canRead()) {
                logcat { "Found real file path: $path" }
                ParcelFileDescriptor.adoptFd(fd).close() // we don't need that anymore
                return path
            }
        } catch (e: Exception) { }
        // Else, pass the fd to mpv
        return "fdclose://$fd"
    }

    private fun setHttpOptions(video: Video) {
        if (presenter.isEpisodeOnline() != true) return
        val source = presenter.source as AnimeHttpSource

        val headers = video.headers?.toMultimap()
            ?.mapValues { it.value.getOrNull(0) ?: "" }
            ?.toMutableMap()
            ?: source.headers.toMultimap()
                .mapValues { it.value.getOrNull(0) ?: "" }
                .toMutableMap()

        val httpHeaderString = headers.map {
            it.key + ": " + it.value.replace(",", "\\,")
        }.joinToString(",")

        MPVLib.setOptionString("http-header-fields", httpHeaderString)
        headers["user-agent"]?.let { MPVLib.setOptionString("user-agent", it) }

        // need to fix the cache
        // MPVLib.setOptionString("cache-on-disk", "yes")
        // val cacheDir = File(applicationContext.filesDir, "media").path
        // MPVLib.setOptionString("cache-dir", cacheDir)
    }

    private fun clearTracks() {
        val count = MPVLib.getPropertyInt("track-list/count")!!
        // Note that because events are async, properties might disappear at any moment
        // so use ?: continue instead of !!
        for (i in 0 until count) {
            val type = MPVLib.getPropertyString("track-list/$i/type") ?: continue
            if (!player.tracks.containsKey(type)) {
                continue
            }
            val mpvId = MPVLib.getPropertyInt("track-list/$i/id") ?: continue
            when (type) {
                "video" -> MPVLib.command(arrayOf("video-remove", "$mpvId"))
                "audio" -> MPVLib.command(arrayOf("audio-remove", "$mpvId"))
                "sub" -> MPVLib.command(arrayOf("sub-remove", "$mpvId"))
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun fileLoaded() {
        MPVLib.setPropertyDouble("speed", preferences.getPlayerSpeed().toDouble())
        clearTracks()
        player.loadTracks()
        subTracks += player.tracks.getValue("sub")
            .drop(1).map { track ->
                Track(track.mpvId.toString(), track.name)
            }.toTypedArray()
        audioTracks += player.tracks.getValue("audio")
            .drop(1).map { track ->
                Track(track.mpvId.toString(), track.name)
            }.toTypedArray()
        if (hadPreviousSubs) {
            subTracks.getOrNull(selectedSub)?.let { sub ->
                MPVLib.command(arrayOf("sub-add", sub.url, "select", sub.url))
            }
        } else {
            currentVideoList?.getOrNull(currentQuality)
                ?.subtitleTracks?.let { tracks ->
                    val langIndex = tracks.indexOfFirst {
                        it.lang.contains(langName)
                    }
                    val requestedLanguage = if (langIndex == -1) 0 else langIndex
                    tracks.getOrNull(requestedLanguage)?.let { sub ->
                        hadPreviousSubs = true
                        selectedSub = requestedLanguage + 1
                        MPVLib.command(arrayOf("sub-add", sub.url, "select", sub.url))
                    }
                } ?: run {
                val mpvSub = player.tracks.getValue("sub").first { player.sid == it.mpvId }
                selectedSub = subTracks.indexOfFirst { it.url == mpvSub.mpvId.toString() }
                    .coerceAtLeast(0)
            }
        }
        if (hadPreviousAudio) {
            audioTracks.getOrNull(selectedAudio)?.let { audio ->
                MPVLib.command(arrayOf("audio-add", audio.url, "select", audio.url))
            }
        } else {
            currentVideoList?.getOrNull(currentQuality)
                ?.audioTracks?.let { tracks ->
                    val langIndex = tracks.indexOfFirst {
                        it.lang.contains(langName)
                    }
                    val requestedLanguage = if (langIndex == -1) 0 else langIndex
                    tracks.getOrNull(requestedLanguage)?.let { audio ->
                        hadPreviousAudio = true
                        selectedAudio = requestedLanguage + 1
                        MPVLib.command(arrayOf("audio-add", audio.url, "select", audio.url))
                    }
                } ?: run {
                val mpvAudio = player.tracks.getValue("audio").first { player.aid == it.mpvId }
                selectedAudio = audioTracks.indexOfFirst { it.url == mpvAudio.mpvId.toString() }
                    .coerceAtLeast(0)
            }
        }
        launchUI { showLoadingIndicator(false) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) player.paused?.let { updatePictureInPictureActions(!it) }
        if (player.videoW!! / player.videoH!! >= 1) {
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            launchUI {
                binding.playerControls.binding.controlsTopLandscape.visibility = View.VISIBLE
                binding.playerControls.binding.controlsTopPortrait.visibility = View.GONE
            }
        } else {
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            launchUI {
                binding.playerControls.binding.controlsTopPortrait.visibility = View.VISIBLE
                binding.playerControls.binding.controlsTopLandscape.visibility = View.GONE
            }
        }
    }

    // mpv events

    private fun eventPropertyUi(property: String, value: Long) {
        when (property) {
            "time-pos" -> binding.playerControls.updatePlaybackPos(value.toInt())
            "duration" -> binding.playerControls.updatePlaybackDuration(value.toInt())
        }
    }

    @Suppress("DEPRECATION")
    private fun eventPropertyUi(property: String, value: Boolean) {
        when (property) {
            "pause" -> {
                setAudioFocus(value)
                updatePlaybackStatus(value)
            }
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

    override fun event(eventId: Int) {
        when (eventId) {
            MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> fileLoaded()
        }
    }
}

private const val ACTION_MEDIA_CONTROL = "media_control"
private const val EXTRA_CONTROL_TYPE = "control_type"
private const val REQUEST_PLAY = 1
private const val REQUEST_PAUSE = 2
private const val CONTROL_TYPE_PLAY = 1
private const val CONTROL_TYPE_PAUSE = 2
private const val REQUEST_PREVIOUS = 3
private const val REQUEST_NEXT = 4
private const val CONTROL_TYPE_PREVIOUS = 3
private const val CONTROL_TYPE_NEXT = 4
