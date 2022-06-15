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
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.DisplayMetrics
import android.util.Rational
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.databinding.PlayerActivityBinding
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.powerManager
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

        fun newIntent(context: Context, animeId: Long?, episodeId: Long?): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra("anime", animeId)
                putExtra("episode", episodeId)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        fun newIntent(context: Context, anime: Anime, episode: Episode): Intent {
            return newIntent(context, anime.id, episode.id)
        }
    }

    override fun onNewIntent(intent: Intent) {
        val anime = intent.extras!!.getLong("anime", -1)
        val episode = intent.extras!!.getLong("episode", -1)
        if (anime == -1L || episode == -1L) {
            finish()
            return
        }
        launchIO {
            presenter.saveEpisodeProgress(player.timePos, player.duration)
            presenter.saveEpisodeHistory()
        }

        presenter.anime = null
        presenter.init(anime, episode)
        super.onNewIntent(intent)
    }

    private var isInPipMode: Boolean = false
    private var isPipStarted: Boolean = false

    private var mReceiver: BroadcastReceiver? = null

    lateinit var binding: PlayerActivityBinding

    private val langName = LocaleHelper.getSimpleLocaleDisplay(preferences.lang().get())

    internal val player get() = binding.player

    private val playerControls get() = binding.playerControls

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
            diffSeekMain = 0
        }
    }

    // Fade out Volume Bar
    private val volumeViewRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.fade_out_medium).also { fadeAnimation ->
            playerControls.binding.volumeView.startAnimation(fadeAnimation)
            playerControls.binding.volumeView.visibility = View.GONE
        }
    }

    // Fade out Brightness Bar
    private val brightnessViewRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.fade_out_medium).also { fadeAnimation ->
            playerControls.binding.brightnessView.startAnimation(fadeAnimation)
            playerControls.binding.brightnessView.visibility = View.GONE
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
                delay = 1000L
            }
            "volume" -> {
                callback = volumeViewRunnable
                itemView = playerControls.binding.volumeView
                delay = 750L
            }
            "brightness" -> {
                callback = brightnessViewRunnable
                itemView = playerControls.binding.brightnessView
                delay = 750L
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
        playerControls.resetControlsFade()
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

        this@PlayerActivity.requestedOrientation = preferences.defaultPlayerOrientationType()

        window.statusBarColor = 70000000
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.navigationBarColor = 70000000
        }

        setVisibilities()

        playerControls.showAndFadeControls()
        toggleAutoplay(preferences.autoplayEnabled().get())

        setMpvConf()
        val logLevel = if (preferences.verboseLogging()) "v" else "warn"
        player.initialize(applicationContext.filesDir.path, logLevel)
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

        if (presenter?.needsInit() == true) {
            val anime = intent.extras!!.getLong("anime", -1)
            val episode = intent.extras!!.getLong("episode", -1)
            if (anime == -1L || episode == -1L) {
                finish()
                return
            }
            presenter.init(anime, episode)
        }
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        width = dm.widthPixels
        height = dm.heightPixels
        if (width <= height) {
            switchOrientation(false)
        } else {
            switchOrientation(true)
        }

        playerIsDestroyed = false
    }

    private fun setMpvConf() {
        val mpvConfFile = File("${applicationContext.filesDir.path}/mpv.conf")
        preferences.mpvConf()?.let { mpvConfFile.writeText(it) }
    }

    /**
     * Class to override [MaterialAlertDialogBuilder] to hide the navigation and status bars
     */
    internal inner class HideBarsMaterialAlertDialogBuilder(context: Context) : MaterialAlertDialogBuilder(context) {
        override fun create(): AlertDialog {
            return super.create().apply {
                val window = this.window ?: return@apply
                val alertWindowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
                alertWindowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                alertWindowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            }
        }

        override fun show(): AlertDialog {
            return super.show().apply {
                val window = this.window ?: return@apply
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            }
        }
    }

    /**
     * Function to handle UI during orientation changes
     */

    private fun switchOrientation(isLandscape: Boolean) {
        launchUI {
            if (isLandscape) {
                if (width <= height) {
                    width = height.also { height = width }
                }

                playerControls.binding.titleMainTxt.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    rightToLeft = playerControls.binding.toggleAutoplay.id
                    rightToRight = ConstraintLayout.LayoutParams.UNSET
                }
                playerControls.binding.titleSecondaryTxt.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    rightToLeft = playerControls.binding.toggleAutoplay.id
                    rightToRight = ConstraintLayout.LayoutParams.UNSET
                }
                playerControls.binding.qualityBtn.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    topToBottom = ConstraintLayout.LayoutParams.UNSET
                }
                playerControls.binding.toggleAutoplay.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    leftToLeft = ConstraintLayout.LayoutParams.UNSET
                    leftToRight = playerControls.binding.titleMainTxt.id
                }
            } else {
                if (width >= height) {
                    width = height.also { height = width }
                }

                playerControls.binding.titleMainTxt.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    rightToLeft = ConstraintLayout.LayoutParams.UNSET
                    rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                }
                playerControls.binding.titleSecondaryTxt.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    rightToLeft = ConstraintLayout.LayoutParams.UNSET
                    rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                }
                playerControls.binding.qualityBtn.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = ConstraintLayout.LayoutParams.UNSET
                    topToBottom = playerControls.binding.backArrowBtn.id
                }
                playerControls.binding.toggleAutoplay.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                    leftToRight = ConstraintLayout.LayoutParams.UNSET
                }
            }
            setupGestures()
            setViewMode()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) player.paused?.let { updatePictureInPictureActions(!it) }
        }
    }

    /**
     * Sets up the gestures to be used
     */

    @Suppress("DEPRECATION")
    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        val gestures = Gestures(this, width.toFloat(), height.toFloat())
        mDetector = GestureDetectorCompat(this, gestures)
        player.setOnTouchListener { v, event ->
            gestures.onTouch(v, event)
            mDetector.onTouchEvent(event)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isPipStarted) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                switchOrientation(true)
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                switchOrientation(false)
            }
        }
    }

    /**
     * Switches to the previous episode if [previous] is true,
     * to the next episode if [previous] is false
     */
    internal fun switchEpisode(previous: Boolean) {
        val switchMethod = if (previous) presenter::previousEpisode else presenter::nextEpisode
        val errorRes = if (previous) R.string.no_previous_episode else R.string.no_next_episode

        launchIO {
            presenter.saveEpisodeProgress(player.timePos, player.duration)
            presenter.saveEpisodeHistory()
        }
        val wasPlayerPaused = player.paused
        player.paused = true
        showLoadingIndicator(true)

        val epTxt = switchMethod {
            if (wasPlayerPaused == false || preferences.autoplayEnabled().get()) {
                player.paused = false
            }
        }

        when {
            epTxt == "Invalid" -> return
            epTxt == null -> { if (presenter.anime != null) launchUI { toast(errorRes) }; showLoadingIndicator(false) }
            isInPipMode -> if (preferences.pipEpisodeToasts()) launchUI { toast(epTxt) }
        }
    }

    // Fade out Player information text
    private val playerInformationRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.fade_out_medium).also { fadeAnimation ->
            playerControls.binding.playerInformation.startAnimation(fadeAnimation)
            playerControls.binding.playerInformation.visibility = View.GONE
        }
    }

    internal fun toggleAutoplay(isAutoplay: Boolean) {
        playerControls.binding.toggleAutoplay.isChecked = isAutoplay
        playerControls.binding.toggleAutoplay.thumbDrawable = if (isAutoplay) {
            ContextCompat.getDrawable(playerControls.context, R.drawable.ic_play_circle_filled_24)
        } else ContextCompat.getDrawable(playerControls.context, R.drawable.ic_pause_circle_filled_24)

        if (isAutoplay) {
            playerControls.binding.playerInformation.text = getString(R.string.enable_auto_play)
        } else {
            playerControls.binding.playerInformation.text = getString(R.string.disable_auto_play)
        }

        if (!preferences.autoplayEnabled().get() == isAutoplay) {
            animationHandler.removeCallbacks(playerInformationRunnable)
            playerControls.binding.playerInformation.visibility = View.VISIBLE
            animationHandler.postDelayed(playerInformationRunnable, 1000L)
        }
        preferences.autoplayEnabled().set(isAutoplay)
    }

    fun toggleControls() = playerControls.toggleControls()

    private fun showLoadingIndicator(visible: Boolean) {
        playerControls.binding.playBtn.isVisible = !visible
        binding.loadingIndicator.isVisible = visible
    }

    private fun isSeeking(seeking: Boolean) {
        val position = player.timePos ?: return
        val cachePosition = MPVLib.getPropertyInt("demuxer-cache-time") ?: -1
        logcat { "pos: $position, cache: $cachePosition" }
        showLoadingIndicator(position >= cachePosition && seeking)
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

                playerControls.binding.playerInformation.text = getString(R.string.video_crop_screen)
            }
            1 -> {
                MPVLib.setOptionString("video-aspect-override", "-1")
                MPVLib.setOptionString("panscan", "0.0")

                playerControls.binding.playerInformation.text = getString(R.string.video_fit_screen)
            }
            0 -> {
                val newAspect = "$width/$height"
                MPVLib.setOptionString("video-aspect-override", newAspect)
                MPVLib.setOptionString("panscan", "0.0")

                playerControls.binding.playerInformation.text = getString(R.string.video_stretch_screen)
            }
        }
        if (playerViewMode != preferences.getPlayerViewMode()) {
            animationHandler.removeCallbacks(playerInformationRunnable)
            playerControls.binding.playerInformation.visibility = View.VISIBLE
            animationHandler.postDelayed(playerInformationRunnable, 1000L)
        }

        preferences.setPlayerViewMode(playerViewMode)
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
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            this.requestedOrientation = preferences.defaultPlayerOrientationLandscape()
        } else {
            this.requestedOrientation = preferences.defaultPlayerOrientationPortrait()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun playPause(view: View) {
        player.cyclePause()
        playerControls.playPause()
    }

    private val doubleTapPlayPauseRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.fade_out_medium).also { fadeAnimation ->
            binding.playPauseView.startAnimation(fadeAnimation)
            binding.playPauseView.visibility = View.GONE
        }
    }

    fun doubleTapPlayPause() {
        animationHandler.removeCallbacks(doubleTapPlayPauseRunnable)
        playPause(playerControls.binding.playBtn)

        if (!playerControls.binding.controlsView.isVisible) {
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
        val y = (event?.y?.toInt() ?: (height / 2)) - v.y.toInt()
        ViewAnimationUtils.createCircularReveal(v, x, y, 0f, kotlin.math.max(v.height, v.width).toFloat()).setDuration(500).start()

        ObjectAnimator.ofFloat(v, "alpha", 0f, 0.2f).setDuration(500).start()
        ObjectAnimator.ofFloat(v, "alpha", 0.2f, 0.2f, 0f).setDuration(1000).start()
        val newPos = (player.timePos ?: 0) + time // only for display
        MPVLib.command(arrayOf("seek", time.toString(), "relative+exact"))

        editSeekText(newPos, time)
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
        playerControls.binding.brightnessText.text = finalBrightness.toString()
        playerControls.binding.brightnessBar.progress = finalBrightness
        playerControls.binding.brightnessBar.secondaryProgress = abs(finalBrightness)
        if (finalBrightness >= 0) {
            playerControls.binding.brightnessImg.setImageResource(R.drawable.ic_brightness_positive_24dp)
            playerControls.binding.brightnessBar.max = 100
        } else {
            playerControls.binding.brightnessImg.setImageResource(R.drawable.ic_brightness_negative_24dp)
            playerControls.binding.brightnessBar.max = 75
        }
        showGestureView("brightness")
    }

    fun verticalScrollRight(diff: Float) {
        fineVolume = (fineVolume + (diff * maxVolume)).coerceIn(0F, maxVolume.toFloat())
        val newVolume = fineVolume.toInt()
        // val newVolumePercent = 100 * newVolume / maxVolume
        audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)

        playerControls.binding.volumeText.text = newVolume.toString()
        playerControls.binding.volumeBar.progress = newVolume
        if (newVolume == 0) playerControls.binding.volumeImg.setImageResource(R.drawable.ic_volume_off_24dp)
        else playerControls.binding.volumeImg.setImageResource(R.drawable.ic_volume_on_24dp)
        showGestureView("volume")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                verticalScrollRight(1 / maxVolume.toFloat())
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                verticalScrollRight(-1 / maxVolume.toFloat())
                return true
            }
            // Not entirely sure how to handle these KeyCodes yet, need to learn some more
            /**
             KeyEvent.KEYCODE_MEDIA_NEXT -> {
             switchEpisode(false)
             return true
             }

             KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
             switchEpisode(true)
             return true
             }
             KeyEvent.KEYCODE_MEDIA_PLAY -> {
             player.paused = true
             doubleTapPlayPause()
             return true
             }
             KeyEvent.KEYCODE_MEDIA_PAUSE -> {
             player.paused = false
             doubleTapPlayPause()
             return true
             }
             KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
             doubleTapPlayPause()
             return true
             }
             */
            else -> {}
        }
        return super.onKeyDown(keyCode, event)
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

        val seekFlags = if (preferences.getPlayerFastSeek()) "absolute+keyframes" else "absolute"

        MPVLib.command(arrayOf("seek", newPos.toString(), seekFlags))
        playerControls.updatePlaybackPos(newPos)

        editSeekText(newPos, newDiff, true)
    }

    private var diffSeekMain = 0
    private var diffSeekHorizontal = 0
    private fun editSeekText(newPos: Int, diff: Int, isHorizontalSeek: Boolean = false) {
        if (isHorizontalSeek) {
            diffSeekMain += diff - diffSeekHorizontal
            diffSeekHorizontal = diff
        } else {
            diffSeekMain += diff
            diffSeekHorizontal = 0
        }
        val diffText = Utils.prettyTime(diffSeekMain, true)
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
        setViewMode()
    }

    private var currentQuality = 0

    @Suppress("UNUSED_PARAMETER")
    fun openQuality(view: View) {
        if (currentVideoList?.isNotEmpty() != true) return
        val qualityAlert = HideBarsMaterialAlertDialogBuilder(this)

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
        playerControls.updateDecoderButton()
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleSpeed(view: View) {
        player.cycleSpeed()
        playerControls.updateSpeedButton()
    }

    @Suppress("UNUSED_PARAMETER")
    fun skipIntro(view: View) {
        doubleTapSeek(85)
    }

    private fun refreshUi() {
        // forces update of entire UI, used when resuming the activity
        val paused = player.paused ?: return
        updatePlaybackStatus(paused)
        player.duration?.let { playerControls.updatePlaybackDuration(it) }
        player.timePos?.let { playerControls.updatePlaybackPos(it) }
        updatePlaylistButtons()
        updateEpisodeText()
        player.loadTracks()
    }

    private fun updateEpisodeText() {
        playerControls.binding.titleMainTxt.text = presenter.anime?.title
        playerControls.binding.titleSecondaryTxt.text = presenter.currentEpisode?.name
    }

    private fun updatePlaylistButtons() {
        val plCount = presenter.episodeList.size
        val plPos = presenter.getCurrentEpisodeIndex()

        val grey = ContextCompat.getColor(this, R.color.tint_disabled)
        val white = ContextCompat.getColor(this, R.color.tint_normal)
        with(playerControls.binding.prevBtn) {
            this.imageTintList = ColorStateList.valueOf(if (plPos == 0) grey else white)
            this.isClickable = plPos != 0
        }
        with(playerControls.binding.nextBtn) {
            this.imageTintList = ColorStateList.valueOf(if (plPos == plCount - 1) grey else white)
            this.isClickable = plPos != plCount - 1
        }
    }

    private fun updatePlaybackStatus(paused: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPipMode) updatePictureInPictureActions(!paused)
        val r = if (paused) R.drawable.ic_play_arrow_80dp else R.drawable.ic_pause_80dp
        playerControls.binding.playBtn.setImageResource(r)

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
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) finishAndRemoveTask()
        super.onBackPressed()
    }

    override fun onStop() {
        launchIO {
            presenter.saveEpisodeHistory()
        }
        if (!playerIsDestroyed) {
            launchIO {
                presenter.saveEpisodeProgress(player.timePos, player.duration)
            }
            player.paused = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            isInPipMode &&
            powerManager.isInteractive
        ) finishAndRemoveTask()

        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        setVisibilities()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) player.paused?.let { updatePictureInPictureActions(!it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        isInPipMode = isInPictureInPictureMode
        isPipStarted = isInPipMode
        playerControls.lockControls(isInPipMode)
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        if (isInPictureInPictureMode) {
            // On Android TV it is required to hide controller in this PIP change callback
            playerControls.hideControls(true)
            binding.loadingIndicator.indicatorSize = binding.loadingIndicator.indicatorSize / 2
            mReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent == null || ACTION_MEDIA_CONTROL != intent.action) {
                        return
                    }
                    when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                        CONTROL_TYPE_PLAY -> {
                            player.paused = false
                        }
                        CONTROL_TYPE_PAUSE -> {
                            player.paused = true
                        }
                        CONTROL_TYPE_PREVIOUS -> {
                            switchEpisode(true)
                        }
                        CONTROL_TYPE_NEXT -> {
                            switchEpisode(false)
                        }
                    }
                }
            }
            registerReceiver(mReceiver, IntentFilter(ACTION_MEDIA_CONTROL))
        } else {
            if (player.paused!!) playerControls.hideControls(false)
            binding.loadingIndicator.indicatorSize = binding.loadingIndicator.indicatorSize * 2
            if (mReceiver != null) {
                unregisterReceiver(mReceiver)
                mReceiver = null
            }
        }
    }

    @Suppress("DEPRECATION", "UNUSED_PARAMETER")
    fun startPiP(view: View) {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            isPipStarted = true
            playerControls.hideControls(true)
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
        var aspect: Int? = null
        if (player.videoAspect != null) {
            aspect = if (player.videoAspect!!.times(10000) >= 23900) 23899 else if (player.videoAspect!!.times(10000) <= 4184) 4185 else player.videoAspect!!.times(10000).toInt()
        }
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
            .setAspectRatio(aspect?.let { Rational(it, 10000) })
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
            presenter.currentEpisode?.let { episode ->
                if (episode.seen && !preferences.preserveWatchingPosition()) episode.last_second_seen = 1L
                MPVLib.command(arrayOf("set", "start", "${episode.last_second_seen / 1000F}"))
            }
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
        subTracks += player.tracks.getOrElse("sub") { emptyList() }
            .drop(1).map { track ->
                Track(track.mpvId.toString(), track.name)
            }.toTypedArray()
        audioTracks += player.tracks.getOrElse("audio") { emptyList() }
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
                val mpvSub = player.tracks.getOrElse("sub") { emptyList() }
                    .first { player.sid == it.mpvId }
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
                val mpvAudio = player.tracks.getOrElse("audio") { emptyList() }
                    .first { player.aid == it.mpvId }
                selectedAudio = audioTracks.indexOfFirst { it.url == mpvAudio.mpvId.toString() }
                    .coerceAtLeast(0)
            }
        }
        launchUI {
            showLoadingIndicator(false)
            if (preferences.adjustOrientationVideoDimensions()) {
                if (player.videoW!! / player.videoH!! >= 1) {
                    this@PlayerActivity.requestedOrientation = preferences.defaultPlayerOrientationLandscape()
                    switchOrientation(true)
                } else {
                    this@PlayerActivity.requestedOrientation = preferences.defaultPlayerOrientationPortrait()
                    switchOrientation(false)
                }
            }
        }
    }

    // mpv events

    private fun eventPropertyUi(property: String, value: Long) {
        when (property) {
            "demuxer-cache-time" -> playerControls.updateBufferPosition(value.toInt())
            "time-pos" -> playerControls.updatePlaybackPos(value.toInt())
            "duration" -> playerControls.updatePlaybackDuration(value.toInt())
        }
    }

    private val nextEpisodeRunnable = Runnable { switchEpisode(false) }

    @Suppress("DEPRECATION")
    private fun eventPropertyUi(property: String, value: Boolean) {
        when (property) {
            "seeking" -> isSeeking(value)
            "paused-for-cache" -> showLoadingIndicator(value)
            "pause" -> {
                if (!isFinishing) {
                    setAudioFocus(value)
                    updatePlaybackStatus(value)
                }
            }
            "eof-reached" -> endFile(value)
        }
    }

    private fun endFile(eofReached: Boolean) {
        animationHandler.removeCallbacks(nextEpisodeRunnable)
        if (eofReached && preferences.autoplayEnabled().get()) {
            animationHandler.postDelayed(nextEpisodeRunnable, 1000L)
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
            MPVLib.mpvEventId.MPV_EVENT_START_FILE -> launchUI { refreshUi() }
        }
    }

    override fun efEvent(err: String?) {
        logcat(LogPriority.ERROR) { err ?: "Error: File ended" }
        runOnUiThread {
            showLoadingIndicator(false)
            toast(err ?: "Error: File ended")
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
