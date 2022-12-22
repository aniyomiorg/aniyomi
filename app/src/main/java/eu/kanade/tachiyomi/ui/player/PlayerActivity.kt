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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
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
import eu.kanade.tachiyomi.databinding.PlayerActivityBinding
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
import eu.kanade.tachiyomi.ui.player.setting.PlayerPreferences
import eu.kanade.tachiyomi.util.AniSkipApi
import eu.kanade.tachiyomi.util.SkipType
import eu.kanade.tachiyomi.util.Stamp
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.powerManager
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import nucleus.factory.RequiresPresenter
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.roundToInt

@RequiresPresenter(PlayerPresenter::class)
class PlayerActivity :
    BaseRxActivity<PlayerPresenter>(),
    MPVLib.EventObserver,
    MPVLib.LogObserver {

    companion object {

        fun newIntent(context: Context, animeId: Long?, episodeId: Long?): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra("anime", animeId)
                putExtra("episode", episodeId)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    private val playerPreferences: PlayerPreferences by injectLazy()

    private val networkPreferences: NetworkPreferences by injectLazy()

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
    internal var deviceSupportsPip: Boolean = false

    internal var isDoubleTapSeeking: Boolean = false

    private var mReceiver: BroadcastReceiver? = null

    lateinit var binding: PlayerActivityBinding

    private val langName = LocaleHelper.getSimpleLocaleDisplayName()

    internal val player get() = binding.player

    val playerControls get() = binding.playerControls

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
    } else {
        null
    }

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

    internal var initialSeek = -1

    private lateinit var mDetector: GestureDetectorCompat

    private val animationHandler = Handler(Looper.getMainLooper())

    // Fade out seek text
    internal val seekTextRunnable = Runnable {
        binding.seekView.visibility = View.GONE
    }

    // Slide out Volume Bar
    internal val volumeViewRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.player_exit_left).also { slideAnimation ->
            if (!playerControls.shouldHideUiForSeek) playerControls.binding.volumeView.startAnimation(slideAnimation)
            playerControls.binding.volumeView.visibility = View.GONE
        }
    }

    // Slide out Brightness Bar
    internal val brightnessViewRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.player_exit_right).also { slideAnimation ->
            if (!playerControls.shouldHideUiForSeek) playerControls.binding.brightnessView.startAnimation(slideAnimation)
            playerControls.binding.brightnessView.visibility = View.GONE
        }
    }

    internal fun showGestureView(type: String) {
        val callback: Runnable
        val itemView: LinearLayout
        val delay: Long
        when (type) {
            "seek" -> {
                callback = seekTextRunnable
                itemView = binding.seekView
                delay = 0L
            }
            "volume" -> {
                callback = volumeViewRunnable
                itemView = playerControls.binding.volumeView
                delay = 750L
                if (!itemView.isVisible) itemView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.player_enter_left))
            }
            "brightness" -> {
                callback = brightnessViewRunnable
                itemView = playerControls.binding.brightnessView
                delay = 750L
                if (!itemView.isVisible) itemView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.player_enter_right))
            }
            else -> return
        }

        animationHandler.removeCallbacks(callback)
        itemView.visibility = View.VISIBLE
        animationHandler.postDelayed(callback, delay)
    }

    private var currentVideoList: List<Video>? = null

    private var playerViewMode: Int = playerPreferences.playerViewMode().get()

    private var playerIsDestroyed = true

    private var subTracks: Array<Track> = emptyArray()

    private var selectedSub = 0

    private var hadPreviousSubs = false

    private var audioTracks: Array<Track> = emptyArray()

    private var selectedAudio = 0

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) deviceSupportsPip = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

        binding = PlayerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this@PlayerActivity.requestedOrientation = playerPreferences.defaultPlayerOrientationType().get()

        window.statusBarColor = 70000000
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.navigationBarColor = 70000000
        }

        setVisibilities()

        if (playerPreferences.hideControls().get()) {
            playerControls.hideControls(true)
        } else {
            playerControls.showAndFadeControls()
        }
        toggleAutoplay(playerPreferences.autoplayEnabled().get())

        setMpvConf()
        val logLevel = if (networkPreferences.verboseLogging().get()) "info" else "warn"
        player.initialize(applicationContext.filesDir.path, logLevel)
        val hwDec = playerPreferences.standardHwDec().get()
        MPVLib.setOptionString("hwdec", hwDec)
        MPVLib.setOptionString("keep-open", "always")
        MPVLib.setOptionString("ytdl", "no")
        MPVLib.addLogObserver(this)
        player.addObserver(this)

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            launchUI { toast(throwable.message) }
            logcat(LogPriority.ERROR, throwable)
            finish()
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        fineVolume = if (playerPreferences.playerVolumeValue().get() == -1.0F) audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()else playerPreferences.playerVolumeValue().get()
        verticalScrollRight(0F)
        maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        playerControls.binding.volumeBar.max = maxVolume
        playerControls.binding.volumeBar.secondaryProgress = maxVolume

        brightness = if (playerPreferences.playerBrightnessValue().get() == -1.0F) Utils.getScreenBrightness(this) ?: 0.5F else playerPreferences.playerBrightnessValue().get()
        verticalScrollLeft(0F)

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
        playerPreferences.mpvConf().get().let { mpvConfFile.writeText(it) }
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
            setVisibilities()
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
                playerControls.binding.playerOverflow.updateLayoutParams<ConstraintLayout.LayoutParams> {
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
                playerControls.binding.playerOverflow.updateLayoutParams<ConstraintLayout.LayoutParams> {
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
            if (deviceSupportsPip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) player.paused?.let { updatePictureInPictureActions(!it) }
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
    internal fun switchEpisode(previous: Boolean, autoPlay: Boolean = false) {
        val switchMethod = if (previous && !autoPlay) {
            { callback: () -> Unit -> presenter.previousEpisode(player.timePos, player.duration, callback) }
        } else {
            { callback: () -> Unit -> presenter.nextEpisode(player.timePos, player.duration, callback, autoPlay) }
        }
        val errorRes = if (previous) R.string.no_previous_episode else R.string.no_next_episode

        val wasPlayerPaused = player.paused
        player.paused = true
        showLoadingIndicator(true)

        val epTxt = switchMethod {
            if (wasPlayerPaused == false || autoPlay) {
                player.paused = false
            }
        }

        when {
            epTxt == "Invalid" -> return
            epTxt == null -> {
                if (presenter.anime != null && !autoPlay) {
                    launchUI { toast(errorRes) }
                }
                showLoadingIndicator(false)
            }
            isInPipMode -> {
                if (playerPreferences.pipEpisodeToasts().get()) {
                    launchUI { toast(epTxt) }
                }
            }
        }
    }

    // Fade out Player information text
    private val playerInformationRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.fade_out_short).also { fadeAnimation ->
            playerControls.binding.playerInformation.startAnimation(fadeAnimation)
            playerControls.binding.playerInformation.visibility = View.GONE
        }
    }

    internal fun toggleAutoplay(isAutoplay: Boolean) {
        playerControls.binding.toggleAutoplay.isChecked = isAutoplay
        playerControls.binding.toggleAutoplay.thumbDrawable = if (isAutoplay) {
            ContextCompat.getDrawable(playerControls.context, R.drawable.ic_play_circle_filled_24)
        } else {
            ContextCompat.getDrawable(playerControls.context, R.drawable.ic_pause_circle_filled_24)
        }

        if (isAutoplay) {
            playerControls.binding.playerInformation.text = getString(R.string.enable_auto_play)
        } else {
            playerControls.binding.playerInformation.text = getString(R.string.disable_auto_play)
        }

        if (!playerPreferences.autoplayEnabled().get() == isAutoplay) {
            animationHandler.removeCallbacks(playerInformationRunnable)
            playerControls.binding.playerInformation.visibility = View.VISIBLE
            animationHandler.postDelayed(playerInformationRunnable, 1000L)
        }
        playerPreferences.autoplayEnabled().set(isAutoplay)
    }

    fun toggleControls() = playerControls.toggleControls()

    private fun showLoadingIndicator(visible: Boolean) {
        playerControls.binding.playBtn.isVisible = !visible
        binding.loadingIndicator.isVisible = visible
    }

    private fun isSeeking(seeking: Boolean) {
        val position = player.timePos ?: return
        val cachePosition = MPVLib.getPropertyInt("demuxer-cache-time") ?: -1
        showLoadingIndicator(position >= cachePosition && seeking)
    }

    private fun setSub(index: Int) {
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

    private fun setAudio(index: Int) {
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
        if (playerViewMode != playerPreferences.playerViewMode().get()) {
            animationHandler.removeCallbacks(playerInformationRunnable)
            playerControls.binding.playerInformation.visibility = View.VISIBLE
            animationHandler.postDelayed(playerInformationRunnable, 1000L)
        }

        playerPreferences.playerViewMode().set(playerViewMode)
    }

    @Suppress("DEPRECATION")
    fun setVisibilities() {
        binding.root.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LOW_PROFILE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && playerPreferences.playerFullscreen().get()) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @SuppressLint("SourceLockedOrientationActivity")
    fun rotatePlayer(view: View) {
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            this.requestedOrientation = playerPreferences.defaultPlayerOrientationLandscape().get()
        } else {
            this.requestedOrientation = playerPreferences.defaultPlayerOrientationPortrait().get()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun playPause(view: View) {
        player.cyclePause()
        playerControls.playPause()
    }

    private val doubleTapPlayPauseRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.player_fade_out).also { fadeAnimation ->
            binding.playPauseView.startAnimation(fadeAnimation)
            binding.playPauseView.visibility = View.GONE
        }
    }

    fun doubleTapPlayPause() {
        animationHandler.removeCallbacks(doubleTapPlayPauseRunnable)
        playPause(playerControls.binding.playBtn)

        if (!playerControls.binding.controlsView.isVisible) {
            when {
                player.paused!! -> { binding.playPauseView.setImageResource(R.drawable.ic_pause_72dp) }
                !player.paused!! -> { binding.playPauseView.setImageResource(R.drawable.ic_play_arrow_72dp) }
            }

            AnimationUtils.loadAnimation(this, R.anim.player_fade_in).also { fadeAnimation ->
                binding.playPauseView.startAnimation(fadeAnimation)
                binding.playPauseView.visibility = View.VISIBLE
            }

            animationHandler.postDelayed(doubleTapPlayPauseRunnable, 500L)
        } else {
            binding.playPauseView.visibility = View.GONE
        }
    }

    private lateinit var doubleTapBg: ImageView

    private val doubleTapSeekRunnable = Runnable {
        isDoubleTapSeeking = false
        binding.secondsView.isVisible = false
        doubleTapBg.isVisible = false
        binding.secondsView.seconds = 0
        binding.secondsView.stop()
    }

    fun doubleTapSeek(time: Int, event: MotionEvent? = null, isDoubleTap: Boolean = true) {
        if (!isDoubleTapSeeking) doubleTapBg = if (time < 0) binding.rewBg else binding.ffwdBg
        val v = if (time < 0) binding.rewTap else binding.ffwdTap
        val w = if (time < 0) width * 0.2F else width * 0.8F
        val x = (event?.x?.toInt() ?: w.toInt()) - v.x.toInt()
        val y = (event?.y?.toInt() ?: (height / 2)) - v.y.toInt()

        isDoubleTapSeeking = isDoubleTap
        binding.secondsView.isVisible = true
        animationHandler.removeCallbacks(doubleTapSeekRunnable)
        animationHandler.postDelayed(doubleTapSeekRunnable, 750L)

        if (time < 0) {
            binding.secondsView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.UNSET
                leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            }
            binding.secondsView.isForward = false

            if (doubleTapBg != binding.rewBg) {
                doubleTapBg.isVisible = false
                binding.secondsView.seconds = 0
                doubleTapBg = binding.rewBg
            }
            doubleTapBg.isVisible = true

            binding.secondsView.seconds -= time
        } else {
            binding.secondsView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                leftToLeft = ConstraintLayout.LayoutParams.UNSET
            }
            binding.secondsView.isForward = true

            if (doubleTapBg != binding.ffwdBg) {
                doubleTapBg.isVisible = false
                binding.secondsView.seconds = 0
                doubleTapBg = binding.ffwdBg
            }
            doubleTapBg.isVisible = true

            binding.secondsView.seconds += time
        }
        playerControls.hideUiForSeek()
        binding.secondsView.start()
        ViewAnimationUtils.createCircularReveal(v, x, y, 0f, kotlin.math.max(v.height, v.width).toFloat()).setDuration(500).start()

        ObjectAnimator.ofFloat(v, "alpha", 0f, 0.15f).setDuration(500).start()
        ObjectAnimator.ofFloat(v, "alpha", 0.15f, 0.15f, 0f).setDuration(1000).start()

        MPVLib.command(arrayOf("seek", time.toString(), "relative+exact"))
    }

    fun verticalScrollLeft(diff: Float) {
        if (diff != 0F) brightness = (brightness + diff).coerceIn(-0.75F, 1F)
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
        playerControls.binding.brightnessBar.progress = abs(finalBrightness)
        if (finalBrightness >= 0) {
            playerControls.binding.brightnessImg.setImageResource(R.drawable.ic_brightness_positive_24dp)
            playerControls.binding.brightnessBar.max = 100
            playerControls.binding.brightnessBar.secondaryProgress = 100
        } else {
            playerControls.binding.brightnessImg.setImageResource(R.drawable.ic_brightness_negative_24dp)
            playerControls.binding.brightnessBar.max = 75
            playerControls.binding.brightnessBar.secondaryProgress = 75
        }
        if (diff != 0F) showGestureView("brightness")
    }

    fun verticalScrollRight(diff: Float) {
        if (diff != 0F) fineVolume = (fineVolume + (diff * maxVolume)).coerceIn(0F, maxVolume.toFloat())
        val newVolume = fineVolume.toInt()
        // val newVolumePercent = 100 * newVolume / maxVolume
        audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)

        playerControls.binding.volumeText.text = newVolume.toString()
        playerControls.binding.volumeBar.progress = newVolume
        if (newVolume == 0) {
            playerControls.binding.volumeImg.setImageResource(R.drawable.ic_volume_off_24dp)
        } else {
            playerControls.binding.volumeImg.setImageResource(R.drawable.ic_volume_on_24dp)
        }
        if (diff != 0F) showGestureView("volume")
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

    fun horizontalScroll(diff: Float, final: Boolean = false) {
        // disable seeking when timePos is not available
        val duration = player.duration ?: 0
        if (duration == 0 || initialSeek < 0) {
            return
        }
        val newPos = (initialSeek + diff.toInt()).coerceIn(0, duration)
        val newDiff = newPos - initialSeek

        playerControls.hideUiForSeek()
        if (playerPreferences.playerSmoothSeek().get() && final) player.timePos = newPos else MPVLib.command(arrayOf("seek", newPos.toString(), "absolute+keyframes"))
        playerControls.updatePlaybackPos(newPos)

        val diffText = Utils.prettyTime(newDiff, true)
        binding.seekText.text = getString(R.string.ui_seek_distance, Utils.prettyTime(newPos), diffText)
        showGestureView("seek")
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

    @Suppress("UNUSED_PARAMETER")
    fun pickAudio(view: View) {
        val audioTracks = audioTracks.takeUnless { it.isEmpty() } ?: return

        playerControls.hideControls(true)
        PlayerTracksSheet(
            this,
            R.string.audio_dialog_header,
            ::setAudio,
            audioTracks,
            selectedAudio,
        ).show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun pickSub(view: View) {
        val subTracks = subTracks.takeUnless { it.isEmpty() } ?: return

        playerControls.hideControls(true)
        PlayerTracksSheet(
            this,
            R.string.subtitle_dialog_header,
            ::setSub,
            subTracks,
            selectedSub,
        ).show()
    }

    private var currentQuality = 0

    @Suppress("UNUSED_PARAMETER")
    fun pickQuality(view: View) {
        val videoTracks = currentVideoList?.map {
            Track("", it.quality)
        }?.toTypedArray()?.takeUnless { it.isEmpty() } ?: return

        playerControls.hideControls(true)
        PlayerTracksSheet(
            this,
            R.string.quality_dialog_header,
            ::changeQuality,
            videoTracks,
            currentQuality,
        ).show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun openOptions(view: View) {
        playerControls.hideControls(true)
        PlayerOptionsSheet(this).show()
    }

    var stats: Boolean = false
    var statsPage: Int = 0
        set(value) {
            val newValue = when (value) {
                0 -> 1
                1 -> 2
                2 -> 3
                else -> 1
            }
            if (!stats) toggleStats()
            MPVLib.command(arrayOf("script-binding", "stats/display-page-$newValue"))
            field = newValue - 1
        }

    fun toggleStats() {
        MPVLib.command(arrayOf("script-binding", "stats/display-stats-toggle"))
        stats = !stats
    }

    var gestureVolumeBrightness: Boolean = playerPreferences.gestureVolumeBrightness().get()
        set(value) {
            playerPreferences.gestureVolumeBrightness().set(value)
            field = value
        }

    var gestureHorizontalSeek: Boolean = playerPreferences.gestureHorizontalSeek().get()
        set(value) {
            playerPreferences.gestureHorizontalSeek().set(value)
            field = value
        }

    var screenshotSubs: Boolean = playerPreferences.screenshotSubtitles().get()
        set(value) {
            playerPreferences.screenshotSubtitles().set(value)
            field = value
        }

    fun takeScreenshot(): InputStream? {
        val filename = cacheDir.path + "/${System.currentTimeMillis()}_mpv_screenshot_tmp.png"
        val subtitleFlag = if (screenshotSubs) {
            "subtitles"
        } else {
            "video"
        }
        MPVLib.command(arrayOf("screenshot-to-file", filename, subtitleFlag))
        val tempFile = File(filename).takeIf { it.exists() } ?: return null
        val newFile = File(cacheDir.path + "/mpv_screenshot.png")
        newFile.delete()
        tempFile.renameTo(newFile)
        return newFile.takeIf { it.exists() }?.inputStream()
    }

    /**
     * Called from the options sheet. It delegates the call to the presenter to do some IO, which
     * will call [onShareImageResult] with the path the image was saved on when it's ready.
     */
    fun shareImage() {
        presenter.shareImage()
    }

    /**
     * Called from the presenter when a screenshot is ready to be shared. It shows Android's
     * default sharing tool.
     */
    fun onShareImageResult(uri: Uri, seconds: String) {
        val anime = presenter.anime ?: return
        val episode = presenter.currentEpisode ?: return

        val intent = uri.toShareIntent(
            context = applicationContext,
            message = getString(R.string.share_screenshot_info, anime.title, episode.name, seconds),
        )
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    /**
     * Called from the options sheet. It delegates saving the screenshot on
     * external storage to the presenter.
     */
    fun saveImage() {
        presenter.saveImage()
    }

    /**
     * Called from the presenter when a screenshot is saved or fails. It shows a message
     * or logs the event depending on the [result].
     */
    fun onSaveImageResult(result: PlayerPresenter.SaveImageResult) {
        when (result) {
            is PlayerPresenter.SaveImageResult.Success -> {
                toast(R.string.picture_saved)
            }
            is PlayerPresenter.SaveImageResult.Error -> {
                logcat(LogPriority.ERROR, result.error)
            }
        }
    }

    /**
     * Called from the options sheet. It delegates setting the screenshot
     * as the cover to the presenter.
     */
    fun setAsCover() {
        presenter.setAsCover(this)
    }

    /**
     * Called from the presenter when a screenshot is set as cover or fails.
     * It shows a different message depending on the [result].
     */
    fun onSetAsCoverResult(result: PlayerPresenter.SetAsCoverResult) {
        toast(
            when (result) {
                PlayerPresenter.SetAsCoverResult.Success -> R.string.cover_updated
                PlayerPresenter.SetAsCoverResult.AddToLibraryFirst -> R.string.notification_first_add_to_library
                PlayerPresenter.SetAsCoverResult.Error -> R.string.notification_cover_update_failed
            },
        )
    }

    private fun changeQuality(quality: Int) {
        if (playerIsDestroyed) return
        if (currentQuality == quality) return
        logcat(LogPriority.INFO) { "changing quality" }
        currentVideoList?.getOrNull(quality)?.let {
            currentQuality = quality
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
        val standardHwDec = playerPreferences.standardHwDec().get()
        val currentHwDec = player.hwdecActive

        if (standardHwDec == currentHwDec) {
            val hwDecEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                "mediacodec"
            } else {
                "mediacodec-copy"
            }
            val otherHwDec = when (standardHwDec) {
                "no" -> hwDecEnabled
                else -> "no"
            }
            MPVLib.setPropertyString("hwdec", otherHwDec)
        } else {
            MPVLib.setOptionString("hwdec", standardHwDec)
        }
        playerControls.updateDecoderButton()
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleSpeed(view: View) {
        player.cycleSpeed()
        playerControls.updateSpeedButton()
    }

    @Suppress("UNUSED_PARAMETER")
    fun skipIntro(view: View) {
        if (skipType != null) {
            // this stop the counter
            if (waitingAniSkip > 0) {
                waitingAniSkip = -1
                return
            }
            skipType.let { MPVLib.command(arrayOf("seek", "${aniSkipInterval!!.first{it.skipType == skipType}.interval.endTime}", "absolute")) }
            AniSkipApi.PlayerUtils(binding, aniSkipInterval!!).skipAnimation(skipType!!)
        } else if (playerControls.binding.controlsSkipIntroBtn.text != "") {
            doubleTapSeek(presenter.getAnimeSkipIntroLength(), isDoubleTap = false)
            playerControls.resetControlsFade()
        }
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
        playerControls.binding.controlsSkipIntroBtn.text = getString(R.string.player_controls_skip_intro_text, presenter.getAnimeSkipIntroLength())
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
        if (deviceSupportsPip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPipMode) updatePictureInPictureActions(!paused)
        val r = if (paused) R.drawable.ic_play_arrow_72dp else R.drawable.ic_pause_72dp
        playerControls.binding.playBtn.setImageResource(r)

        if (paused) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDestroy() {
        playerPreferences.playerVolumeValue().set(fineVolume)
        playerPreferences.playerBrightnessValue().set(brightness)
        presenter.deletePendingEpisodes()
        MPVLib.removeLogObserver(this)
        if (!playerIsDestroyed) {
            playerIsDestroyed = true
            player.removeObserver(this)
            player.destroy()
        }
        abandonAudioFocus()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (deviceSupportsPip) {
            if (player.paused == false && playerPreferences.pipOnExit().get()) {
                startPiP()
            } else {
                finishAndRemoveTask()
                super.onBackPressed()
            }
        } else {
            finishAndRemoveTask()
            super.onBackPressed()
        }
    }

    override fun onUserLeaveHint() {
        if (player.paused == false && playerPreferences.pipOnExit().get()) startPiP()
        super.onUserLeaveHint()
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
        if (deviceSupportsPip && isInPipMode &&
            powerManager.isInteractive
        ) {
            finishAndRemoveTask()
        }

        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        setVisibilities()
        if (deviceSupportsPip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) player.paused?.let { updatePictureInPictureActions(!it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        isInPipMode = isInPictureInPictureMode
        isPipStarted = isInPipMode
        playerControls.lockControls(isInPipMode)
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

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

    @Suppress("DEPRECATION")
    fun startPiP() {
        if (isInPipMode) return
        if (deviceSupportsPip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

    fun setVideoList(videos: List<Video>, fromStart: Boolean = false) {
        if (playerIsDestroyed) return
        currentVideoList = videos
        currentVideoList?.firstOrNull()?.let {
            setHttpOptions(it)
            presenter.currentEpisode?.let { episode ->
                if ((episode.seen && !playerPreferences.preserveWatchingPosition().get()) || fromStart) episode.last_second_seen = 1L
                MPVLib.command(arrayOf("set", "start", "${episode.last_second_seen / 1000F}"))
                playerControls.updatePlaybackDuration(episode.total_seconds.toInt() / 1000)
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
        headers["referer"]?.let { MPVLib.setOptionString("referrer", it) }

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
        MPVLib.setPropertyDouble("speed", playerPreferences.playerSpeed().get().toDouble())
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
            if (playerPreferences.adjustOrientationVideoDimensions().get()) {
                if ((player.videoW ?: 1) / (player.videoH ?: 1) >= 1) {
                    this@PlayerActivity.requestedOrientation = playerPreferences.defaultPlayerOrientationLandscape().get()
                    switchOrientation(true)
                } else {
                    this@PlayerActivity.requestedOrientation = playerPreferences.defaultPlayerOrientationPortrait().get()
                    switchOrientation(false)
                }
            }
        }
        // aniSkip stuff
        waitingAniSkip = playerPreferences.waitingTimeAniSkip().get().toInt()
        runBlocking {
            aniSkipInterval = presenter.aniSkipResponse()
        }
    }

    private val aniSkipEnable = playerPreferences.aniSkipEnabled().get()
    private val autoSkipAniSkip = playerPreferences.autoSkipAniSkip().get()
    private val netflixStyle = playerPreferences.enableNetflixStyleAniSkip().get()

    private var aniSkipInterval: List<Stamp>? = null
    private var waitingAniSkip = playerPreferences.waitingTimeAniSkip().get()

    var skipType: SkipType? = null

    private fun aniSkipStuff(value: Long) {
        if (aniSkipEnable) {
            // if it doesn't find the opening it will show the +85 button
            val showNormalSkipButton = aniSkipInterval?.firstOrNull { it.skipType == SkipType.OP || it.skipType == SkipType.MIXED_OP } == null
            if (showNormalSkipButton) return

            skipType = aniSkipInterval?.firstOrNull { it.interval.startTime <= value && it.interval.endTime > value }?.skipType
            skipType?.let { skipType ->
                val aniSkipPlayerUtils = AniSkipApi.PlayerUtils(binding, aniSkipInterval!!)
                if (netflixStyle) {
                    // show a toast with the seconds before the skip
                    if (waitingAniSkip == playerPreferences.waitingTimeAniSkip().get()) {
                        toast("AniSkip: ${getString(R.string.player_aniskip_dontskip_toast,waitingAniSkip)}")
                    }
                    aniSkipPlayerUtils.showSkipButton(skipType, waitingAniSkip)
                    waitingAniSkip--
                } else if (autoSkipAniSkip) {
                    skipType.let { MPVLib.command(arrayOf("seek", "${aniSkipInterval!!.first{it.skipType == skipType}.interval.endTime}", "absolute")) }
                } else {
                    aniSkipPlayerUtils.showSkipButton(skipType)
                }
            } ?: run {
                launchUI {
                    playerControls.binding.controlsSkipIntroBtn.isVisible = false
                }
            }
        }
    }

    // mpv events

    private fun eventPropertyUi(property: String, value: Long) {
        when (property) {
            "demuxer-cache-time" -> playerControls.updateBufferPosition(value.toInt())
            "time-pos" -> {
                playerControls.updatePlaybackPos(value.toInt())
                aniSkipStuff(value)
            }
            "duration" -> playerControls.updatePlaybackDuration(value.toInt())
        }
    }

    private val nextEpisodeRunnable = Runnable { switchEpisode(previous = false, autoPlay = true) }

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
        if (eofReached && playerPreferences.autoplayEnabled().get()) {
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
        var errorMessage = err ?: "Error: File ended"
        if (!httpError.isNullOrEmpty()) {
            errorMessage += ": $httpError"
            httpError = null
        }
        logcat(LogPriority.ERROR) { errorMessage }
        runOnUiThread {
            showLoadingIndicator(false)
            toast(errorMessage, Toast.LENGTH_LONG)
        }
    }

    private var httpError: String? = null

    override fun logMessage(prefix: String, level: Int, text: String) {
        val logPriority = when (level) {
            MPVLib.mpvLogLevel.MPV_LOG_LEVEL_FATAL, MPVLib.mpvLogLevel.MPV_LOG_LEVEL_ERROR -> LogPriority.ERROR
            MPVLib.mpvLogLevel.MPV_LOG_LEVEL_WARN -> LogPriority.WARN
            MPVLib.mpvLogLevel.MPV_LOG_LEVEL_INFO -> LogPriority.INFO
            else -> null
        }
        if (logPriority != null) {
            if (text.contains("HTTP error")) httpError = text
            logcat.logcat("mpv/$prefix", logPriority) { text }
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
