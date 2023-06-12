package eu.kanade.tachiyomi.ui.player

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.databinding.PlayerActivityBinding
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.player.settings.PlayerOptionsSheet
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsSheet
import eu.kanade.tachiyomi.ui.player.settings.PlayerTracksSheet
import eu.kanade.tachiyomi.ui.player.viewer.ACTION_MEDIA_CONTROL
import eu.kanade.tachiyomi.ui.player.viewer.AspectState
import eu.kanade.tachiyomi.ui.player.viewer.CONTROL_TYPE_NEXT
import eu.kanade.tachiyomi.ui.player.viewer.CONTROL_TYPE_PAUSE
import eu.kanade.tachiyomi.ui.player.viewer.CONTROL_TYPE_PLAY
import eu.kanade.tachiyomi.ui.player.viewer.CONTROL_TYPE_PREVIOUS
import eu.kanade.tachiyomi.ui.player.viewer.EXTRA_CONTROL_TYPE
import eu.kanade.tachiyomi.ui.player.viewer.Gestures
import eu.kanade.tachiyomi.ui.player.viewer.HwDecState
import eu.kanade.tachiyomi.ui.player.viewer.PictureInPictureHandler
import eu.kanade.tachiyomi.ui.player.viewer.PipState
import eu.kanade.tachiyomi.ui.player.viewer.SeekState
import eu.kanade.tachiyomi.ui.player.viewer.SetAsCover
import eu.kanade.tachiyomi.util.AniSkipApi
import eu.kanade.tachiyomi.util.SkipType
import eu.kanade.tachiyomi.util.Stamp
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.powerManager
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.core.util.lang.launchUI
import tachiyomi.core.util.lang.withUIContext
import tachiyomi.core.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.roundToInt

class PlayerActivity :
    BaseActivity(),
    MPVLib.EventObserver,
    MPVLib.LogObserver {

    internal val viewModel by viewModels<PlayerViewModel>()

    internal val playerPreferences: PlayerPreferences = Injekt.get()

    companion object {
        fun newIntent(context: Context, animeId: Long?, episodeId: Long?): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra("animeId", animeId)
                putExtra("episodeId", episodeId)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        val animeId = intent.extras!!.getLong("animeId", -1)
        val episodeId = intent.extras!!.getLong("episodeId", -1)
        if (animeId == -1L || episodeId == -1L) {
            finish()
            return
        }
        NotificationReceiver.dismissNotification(this, animeId.hashCode(), Notifications.ID_NEW_EPISODES)

        viewModel.saveCurrentEpisodeWatchingProgress()

        lifecycleScope.launchNonCancellable {
            val initResult = viewModel.init(animeId, episodeId)
            if (!initResult.second.getOrDefault(false)) {
                val exception = initResult.second.exceptionOrNull() ?: IllegalStateException("Unknown error")
                withUIContext {
                    setInitialEpisodeError(exception)
                }
            }
            lifecycleScope.launch { setVideoList(initResult.first!!) }
        }
        super.onNewIntent(intent)
    }

    internal val pip = PictureInPictureHandler(this, playerPreferences.enablePip().get())

    private var mReceiver: BroadcastReceiver? = null

    lateinit var binding: PlayerActivityBinding

    private val langName = LocaleHelper.getSimpleLocaleDisplayName()

    internal val player get() = binding.player

    internal val playerControls get() = binding.playerControls

    private var audioManager: AudioManager? = null
    private var fineVolume = 0F
    private var maxVolume = 0

    private var brightness = 0F

    private var deviceWidth = 0
    private var deviceHeight = 0

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

    private var playerSettingsSheet: PlayerSettingsSheet? = null

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

    private val animationHandler = Handler(Looper.getMainLooper())

    // Fade out seek text
    internal val seekTextRunnable = Runnable {
        binding.seekView.visibility = View.GONE
    }

    // Slide out Volume Bar
    internal val volumeViewRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.player_exit_left).also { slideAnimation ->
            if (SeekState.mode != SeekState.SCROLL) playerControls.binding.volumeView.startAnimation(slideAnimation)
            playerControls.binding.volumeView.visibility = View.GONE
        }
    }

    // Slide out Brightness Bar
    internal val brightnessViewRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.player_exit_right).also { slideAnimation ->
            if (SeekState.mode != SeekState.SCROLL) playerControls.binding.brightnessView.startAnimation(slideAnimation)
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

    private var playerViewMode = AspectState.get(playerPreferences.playerViewMode().get())

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
    override fun onCreate(savedInstanceState: Bundle?) {
        registerSecureActivity(this)
        Utils.copyAssets(this)
        super.onCreate(savedInstanceState)

        setupPlayerControls()
        setupPlayerMPV()
        setupPlayerAudio()
        setupPlayerBrightness()
        loadDeviceDimensions()

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            toast(throwable.message)
            logcat(LogPriority.ERROR, throwable)
            finish()
        }

        viewModel.eventFlow
            .onEach { event ->
                when (event) {
                    is PlayerViewModel.Event.SavedImage -> {
                        onSaveImageResult(event.result)
                    }
                    is PlayerViewModel.Event.ShareImage -> {
                        onShareImageResult(event.uri, event.seconds)
                    }
                    is PlayerViewModel.Event.SetCoverResult -> {
                        onSetAsCoverResult(event.result)
                    }
                    is PlayerViewModel.Event.SetAnimeSkipIntro -> {
                        updateEpisodeText()
                    }
                }
            }
            .launchIn(lifecycleScope)

        onNewIntent(this.intent)

        playerIsDestroyed = false
    }

    private fun setupPlayerControls() {
        binding = PlayerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
    }

    private fun setupPlayerMPV() {
        val mpvConfFile = File("${applicationContext.filesDir.path}/mpv.conf")
        playerPreferences.mpvConf().get().let { mpvConfFile.writeText(it) }

        val logLevel = if (viewModel.networkPreferences.verboseLogging().get()) "info" else "warn"
        player.initialize(applicationContext.filesDir.path, logLevel)

        MPVLib.setOptionString("hwdec", playerPreferences.standardHwDec().get())
        HwDecState.mode = HwDecState.get(playerPreferences.standardHwDec().get())
        MPVLib.setOptionString("keep-open", "always")
        MPVLib.setOptionString("ytdl", "no")

        MPVLib.addLogObserver(this)
        player.addObserver(this)
    }

    private fun setupPlayerAudio() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val useDeviceVolume = playerPreferences.playerVolumeValue().get() == -1.0F || !playerPreferences.rememberPlayerVolume().get()
        fineVolume = if (useDeviceVolume) {
            audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        } else {
            playerPreferences.playerVolumeValue().get()
        }

        verticalScrollRight(0F)

        maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        playerControls.binding.volumeBar.max = maxVolume
        playerControls.binding.volumeBar.secondaryProgress = maxVolume

        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    private fun setupPlayerBrightness() {
        val useDeviceBrightness = playerPreferences.playerBrightnessValue().get() == -1.0F || !playerPreferences.rememberPlayerBrightness().get()
        brightness = if (useDeviceBrightness) {
            Utils.getScreenBrightness(this) ?: 0.5F
        } else {
            playerPreferences.playerBrightnessValue().get()
        }
        verticalScrollLeft(0F)
    }

    @Suppress("DEPRECATION")
    private fun loadDeviceDimensions() {
        this@PlayerActivity.requestedOrientation = playerPreferences.defaultPlayerOrientationType().get()
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        deviceWidth = dm.widthPixels
        deviceHeight = dm.heightPixels
        if (deviceWidth <= deviceHeight) {
            switchControlsOrientation(false)
        } else {
            switchControlsOrientation(true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (!isChangingConfigurations) {
            viewModel.onSaveInstanceStateNonConfigurationChange()
        }
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        viewModel.saveCurrentEpisodeWatchingProgress()
        super.onPause()
    }

    /**
     * Function to handle UI during orientation changes
     */
    private fun switchControlsOrientation(isLandscape: Boolean) {
        viewModel.viewModelScope.launchUI {
            setVisibilities()
            if (isLandscape) {
                if (deviceWidth <= deviceHeight) {
                    deviceWidth = deviceHeight.also { deviceHeight = deviceWidth }
                }

                playerControls.binding.episodeListBtn.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    rightToLeft = playerControls.binding.toggleAutoplay.id
                    rightToRight = ConstraintLayout.LayoutParams.UNSET
                }
                playerControls.binding.playerOverflow.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    topToBottom = ConstraintLayout.LayoutParams.UNSET
                }
                playerControls.binding.toggleAutoplay.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    leftToLeft = ConstraintLayout.LayoutParams.UNSET
                    leftToRight = playerControls.binding.episodeListBtn.id
                }
            } else {
                if (deviceWidth >= deviceHeight) {
                    deviceWidth = deviceHeight.also { deviceHeight = deviceWidth }
                }

                playerControls.binding.episodeListBtn.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    rightToLeft = ConstraintLayout.LayoutParams.UNSET
                    rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                }
                playerControls.binding.playerOverflow.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = ConstraintLayout.LayoutParams.UNSET
                    topToBottom = playerControls.binding.episodeListBtn.id
                }
                playerControls.binding.toggleAutoplay.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                    leftToRight = ConstraintLayout.LayoutParams.UNSET
                }
            }
            setupGestures()
            setViewMode(showText = false)
            if (pip.supportedAndEnabled) player.paused?.let { pip.update(!it) }
            if (playerSettingsSheet?.isShowing == true) {
                playerSettingsSheet!!.dismiss()
            }
        }
    }

    /**
     * Sets up the gestures to be used
     */

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        val gestures = Gestures(this, deviceWidth.toFloat(), deviceHeight.toFloat())
        val mDetector = GestureDetectorCompat(this, gestures)
        player.setOnTouchListener { v, event ->
            gestures.onTouch(v, event)
            mDetector.onTouchEvent(event)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (PipState.mode != PipState.STARTED) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                switchControlsOrientation(true)
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                switchControlsOrientation(false)
            }
        }
    }

    /**
     * Switches to the previous episode if [previous] is true,
     * to the next episode if [previous] is false
     */
    internal fun switchEpisode(previous: Boolean, autoPlay: Boolean = false) {
        if (playerSettingsSheet?.isShowing == true) {
            playerSettingsSheet!!.dismiss()
        }
        player.paused = true
        showLoadingIndicator(true)

        lifecycleScope.launch {
            viewModel.mutableState.update {
                it.copy(isLoadingAdjacentEpisode = true)
            }
            val switchMethod =
                if (previous && !autoPlay) {
                    viewModel.previousEpisode()
                } else {
                    viewModel.nextEpisode()
                }

            val errorRes = if (previous) R.string.no_previous_episode else R.string.no_next_episode

            when (switchMethod) {
                null -> {
                    if (viewModel.currentAnime != null && !autoPlay) {
                        launchUI { toast(errorRes) }
                    }
                    showLoadingIndicator(false)
                }
                else -> {
                    if (switchMethod.first != null) {
                        when {
                            switchMethod.first!!.isEmpty() -> setInitialEpisodeError(Exception("Video list is empty."))
                            else -> setVideoList(switchMethod.first!!)
                        }
                    } else {
                        logcat(LogPriority.ERROR) { "Error getting links" }
                    }

                    if (PipState.mode == PipState.ON && playerPreferences.pipEpisodeToasts().get()) {
                        launchUI { toast(switchMethod.second) }
                    }
                }
            }
        }
    }

    // Fade out Player information text
    private val playerInformationRunnable = Runnable {
        AnimationUtils.loadAnimation(this, R.anim.player_fade_out).also { fadeAnimation ->
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

    private fun showLoadingIndicator(visible: Boolean) {
        viewModel.viewModelScope.launchUI {
            playerControls.binding.playBtn.isVisible = !visible
            binding.loadingIndicator.isVisible = visible
        }
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

    private fun setViewMode(showText: Boolean) {
        playerControls.binding.playerInformation.text = getString(playerViewMode.stringRes)
        when (playerViewMode) {
            AspectState.CROP -> {
                mpvUpdateAspect(aspect = "-1", pan = "1.0")
            }
            AspectState.FIT -> {
                mpvUpdateAspect(aspect = "-1", pan = "0.0")
            }
            AspectState.STRETCH -> {
                val newAspect = "$deviceWidth/$deviceHeight"
                mpvUpdateAspect(aspect = newAspect, pan = "1.0")
            }
        }
        if (showText) {
            animationHandler.removeCallbacks(playerInformationRunnable)
            playerControls.binding.playerInformation.visibility = View.VISIBLE
            animationHandler.postDelayed(playerInformationRunnable, 1000L)
        }

        playerPreferences.playerViewMode().set(playerViewMode.index)
    }

    @Suppress("DEPRECATION")
    fun setVisibilities() {
        val windowInsetsController by lazy { WindowInsetsControllerCompat(window, binding.root) }
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

        if (!playerControls.binding.unlockedView.isVisible) {
            when {
                player.paused!! -> { binding.playPauseView.setImageResource(R.drawable.ic_pause_64dp) }
                !player.paused!! -> { binding.playPauseView.setImageResource(R.drawable.ic_play_arrow_64dp) }
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
        SeekState.mode = SeekState.NONE
        binding.secondsView.visibility = View.GONE
        doubleTapBg.visibility = View.GONE
        binding.secondsView.seconds = 0
        binding.secondsView.stop()
    }

    fun doubleTapSeek(time: Int, event: MotionEvent? = null, isDoubleTap: Boolean = true) {
        if (SeekState.mode != SeekState.DOUBLE_TAP) doubleTapBg = if (time < 0) binding.rewBg else binding.ffwdBg
        val v = if (time < 0) binding.rewTap else binding.ffwdTap
        val w = if (time < 0) deviceWidth * 0.2F else deviceWidth * 0.8F
        val x = (event?.x?.toInt() ?: w.toInt()) - v.x.toInt()
        val y = (event?.y?.toInt() ?: (deviceHeight / 2)) - v.y.toInt()

        SeekState.mode = if (isDoubleTap) SeekState.DOUBLE_TAP else SeekState.NONE
        binding.secondsView.visibility = View.VISIBLE
        animationHandler.removeCallbacks(doubleTapSeekRunnable)
        animationHandler.postDelayed(doubleTapSeekRunnable, 750L)

        if (time < 0) {
            binding.secondsView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.UNSET
                leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            }
            binding.secondsView.isForward = false

            if (doubleTapBg != binding.rewBg) {
                doubleTapBg.visibility = View.GONE
                binding.secondsView.seconds = 0
                doubleTapBg = binding.rewBg
            }
            doubleTapBg.visibility = View.VISIBLE

            binding.secondsView.seconds -= time
        } else {
            binding.secondsView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                leftToLeft = ConstraintLayout.LayoutParams.UNSET
            }
            binding.secondsView.isForward = true

            if (doubleTapBg != binding.ffwdBg) {
                doubleTapBg.visibility = View.GONE
                binding.secondsView.seconds = 0
                doubleTapBg = binding.ffwdBg
            }
            doubleTapBg.visibility = View.VISIBLE

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
            binding.brightnessOverlay.visibility = View.VISIBLE
            val alpha = (abs(brightness) * 256).toInt()
            binding.brightnessOverlay.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
        } else {
            binding.brightnessOverlay.visibility = View.GONE
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

    internal fun initSeek() {
        initialSeek = player.timePos ?: -1
    }

    internal fun horizontalScroll(diff: Float, final: Boolean = false) {
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
            AspectState.STRETCH -> AspectState.FIT
            AspectState.FIT -> AspectState.CROP
            AspectState.CROP -> AspectState.STRETCH
        }
        setViewMode(showText = true)
    }

    @Suppress("UNUSED_PARAMETER")
    fun openTrackDialog(view: View) {
        val qualityTracks = currentVideoList?.map { Track("", it.quality) }?.toTypedArray()?.takeUnless { it.isEmpty() }
        val subTracks = subTracks.takeUnless { it.isEmpty() }
        val audioTracks = audioTracks.takeUnless { it.isEmpty() }

        if (qualityTracks == null || subTracks == null || audioTracks == null) return
        if (playerSettingsSheet?.isShowing == true) return

        playerControls.hideControls(true)
        playerSettingsSheet = PlayerSettingsSheet(this@PlayerActivity).apply { show() }
    }

    private var selectedQuality = 0

    internal fun qualityTracksTab(dismissSheet: () -> Unit): PlayerTracksSheet {
        val videoTracks = currentVideoList!!.map {
            Track("", it.quality)
        }.toTypedArray().takeUnless { it.isEmpty() }!!

        return PlayerTracksSheet(
            this,
            ::changeQuality,
            videoTracks,
            selectedQuality,
            dismissSheet,
            null,
        )
    }

    internal fun subtitleTracksTab(dismissTab: () -> Unit): PlayerTracksSheet {
        val subTracks = subTracks.takeUnless { it.isEmpty() }!!

        playerControls.hideControls(true)
        return PlayerTracksSheet(
            this,
            ::setSub,
            subTracks,
            selectedSub,
            dismissTab,
            null,
        )
    }

    internal fun audioTracksTab(dismissTab: () -> Unit): PlayerTracksSheet {
        val audioTracks = audioTracks.takeUnless { it.isEmpty() }!!

        playerControls.hideControls(true)
        return PlayerTracksSheet(
            this,
            ::setAudio,
            audioTracks,
            selectedAudio,
            dismissTab,
            null,
        )
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

    private fun takeScreenshot(): InputStream? {
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
        viewModel.shareImage({ takeScreenshot()!! }, player.timePos)
    }

    /**
     * Called from the presenter when a screenshot is ready to be shared. It shows Android's
     * default sharing tool.
     */
    private fun onShareImageResult(uri: Uri, seconds: String) {
        val anime = viewModel.currentAnime ?: return
        val episode = viewModel.currentEpisode ?: return

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
        viewModel.saveImage({ takeScreenshot()!! }, player.timePos)
    }

    /**
     * Called from the presenter when a screenshot is saved or fails. It shows a message
     * or logs the event depending on the [result].
     */
    private fun onSaveImageResult(result: PlayerViewModel.SaveImageResult) {
        when (result) {
            is PlayerViewModel.SaveImageResult.Success -> {
                toast(R.string.picture_saved)
            }
            is PlayerViewModel.SaveImageResult.Error -> {
                logcat(LogPriority.ERROR, result.error)
            }
        }
    }

    /**
     * Called from the options sheet. It delegates setting the screenshot
     * as the cover to the presenter.
     */
    fun setAsCover() {
        viewModel.setAsCover(takeScreenshot())
    }

    /**
     * Called from the presenter when a screenshot is set as cover or fails.
     * It shows a different message depending on the [result].
     */
    private fun onSetAsCoverResult(result: SetAsCover) {
        toast(
            when (result) {
                SetAsCover.Success -> R.string.cover_updated
                SetAsCover.AddToLibraryFirst -> R.string.notification_first_add_to_library
                SetAsCover.Error -> R.string.notification_cover_update_failed
            },
        )
    }

    private fun changeQuality(quality: Int) {
        if (playerIsDestroyed) return
        if (selectedQuality == quality) return
        showLoadingIndicator(true)
        logcat(LogPriority.INFO) { "Changing quality" }
        currentVideoList?.getOrNull(quality)?.let {
            selectedQuality = quality
            setHttpOptions(it)
            player.timePos?.let {
                MPVLib.command(arrayOf("set", "start", "${player.timePos}"))
            }
            subTracks = arrayOf(Track("nothing", "Off")) + it.subtitleTracks.toTypedArray()
            audioTracks = arrayOf(Track("nothing", "Off")) + it.audioTracks.toTypedArray()
            MPVLib.command(arrayOf("loadfile", parseVideoUrl(it.videoUrl)))
        }
        viewModel.viewModelScope.launchUI { refreshUi() }
    }

    @Suppress("UNUSED_PARAMETER")
    fun switchDecoder(view: View) {
        val newHwDec = when (HwDecState.get(player.hwdecActive)) {
            HwDecState.HW -> if (HwDecState.isHwSupported) HwDecState.HW_PLUS else HwDecState.SW
            HwDecState.HW_PLUS -> HwDecState.SW
            HwDecState.SW -> HwDecState.HW
        }
        MPVLib.setOptionString("hwdec", newHwDec.mpvValue)
        HwDecState.mode = newHwDec
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
            // this stops the counter
            if (waitingAniSkip > 0 && netflixStyle) {
                waitingAniSkip = -1
                return
            }
            skipType.let { MPVLib.command(arrayOf("seek", "${aniSkipInterval!!.first{it.skipType == skipType}.interval.endTime}", "absolute")) }
            AniSkipApi.PlayerUtils(binding, aniSkipInterval!!).skipAnimation(skipType!!)
        } else if (playerControls.binding.controlsSkipIntroBtn.text != "") {
            doubleTapSeek(viewModel.getAnimeSkipIntroLength(), isDoubleTap = false)
            playerControls.resetControlsFade()
        }
    }

    internal suspend fun refreshUi() {
        // forces update of entire UI, used when resuming the activity
        val paused = player.paused ?: return
        updatePlaybackStatus(paused)
        player.duration?.let { playerControls.updatePlaybackDuration(it) }
        player.timePos?.let { playerControls.updatePlaybackPos(it) }
        updatePlaylistButtons()
        updateEpisodeText()
        player.loadTracks()
    }

    private suspend fun updateEpisodeText() {
        val skipIntroText = getString(R.string.player_controls_skip_intro_text, viewModel.getAnimeSkipIntroLength())
        withUIContext {
            playerControls.binding.titleMainTxt.text = viewModel.currentAnime?.title
            playerControls.binding.titleSecondaryTxt.text = viewModel.currentEpisode?.name
            playerControls.binding.controlsSkipIntroBtn.text = skipIntroText
        }
    }

    private suspend fun updatePlaylistButtons() {
        val plCount = viewModel.episodeList.size
        val plPos = viewModel.getCurrentEpisodeIndex()

        val grey = ContextCompat.getColor(this, R.color.tint_disabled)
        val white = ContextCompat.getColor(this, R.color.tint_normal)
        withUIContext {
            with(playerControls.binding.prevBtn) {
                this.imageTintList = ColorStateList.valueOf(if (plPos == 0) grey else white)
                this.isClickable = plPos != 0
            }
            with(playerControls.binding.nextBtn) {
                this.imageTintList =
                    ColorStateList.valueOf(if (plPos == plCount - 1) grey else white)
                this.isClickable = plPos != plCount - 1
            }
        }
    }

    private fun updatePlaybackStatus(paused: Boolean) {
        if (pip.supportedAndEnabled && PipState.mode == PipState.ON) pip.update(!paused)
        val r = if (paused) R.drawable.ic_play_arrow_64dp else R.drawable.ic_pause_64dp
        playerControls.binding.playBtn.setImageResource(r)

        if (paused) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun finishAndRemoveTask() {
        viewModel.deletePendingEpisodes()
        super.finishAndRemoveTask()
    }

    override fun onDestroy() {
        playerPreferences.playerVolumeValue().set(fineVolume)
        playerPreferences.playerBrightnessValue().set(brightness)
        MPVLib.removeLogObserver(this)
        if (!playerIsDestroyed) {
            playerIsDestroyed = true
            player.removeObserver(this)
            player.destroy()
        }
        abandonAudioFocus()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (pip.supportedAndEnabled) {
            if (player.paused == false && playerPreferences.pipOnExit().get()) {
                pip.start()
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
        if (player.paused == false && playerPreferences.pipOnExit().get()) pip.start()
        super.onUserLeaveHint()
    }

    override fun onStop() {
        viewModel.saveCurrentEpisodeWatchingProgress()
        if (!playerIsDestroyed) {
            player.paused = true
        }
        if (pip.supportedAndEnabled && PipState.mode == PipState.ON && powerManager.isInteractive) {
            finishAndRemoveTask()
        }

        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        setVisibilities()
        if (pip.supportedAndEnabled && PipState.mode == PipState.ON) player.paused?.let { pip.update(!it) }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        PipState.mode = if (isInPictureInPictureMode) PipState.ON else PipState.OFF

        playerControls.lockControls(locked = PipState.mode == PipState.ON)
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

        if (PipState.mode == PipState.ON) {
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

    /**
     * Called from the presenter if the initial load couldn't load the videos of the episode. In
     * this case the activity is closed and a toast is shown to the user.
     */
    private fun setInitialEpisodeError(error: Throwable) {
        toast(error.message)
        logcat(LogPriority.ERROR, error)
        finish()
    }

    private fun setVideoList(videos: List<Video>, fromStart: Boolean = false) {
        if (playerIsDestroyed) return
        currentVideoList = videos
        currentVideoList?.firstOrNull()?.let {
            setHttpOptions(it)
            viewModel.currentEpisode?.let { episode ->
                if ((episode.seen && !playerPreferences.preserveWatchingPosition().get()) || fromStart) episode.last_second_seen = 1L
                MPVLib.command(arrayOf("set", "start", "${episode.last_second_seen / 1000F}"))
                playerControls.updatePlaybackDuration(episode.total_seconds.toInt() / 1000)
            }
            subTracks = arrayOf(Track("nothing", "Off")) + it.subtitleTracks.toTypedArray()
            audioTracks = arrayOf(Track("nothing", "Off")) + it.audioTracks.toTypedArray()
            MPVLib.command(arrayOf("loadfile", parseVideoUrl(it.videoUrl)))
        }
        viewModel.viewModelScope.launchUI { refreshUi() }
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
        } catch (_: Exception) {}
        // Else, pass the fd to mpv
        return "fdclose://$fd"
    }

    private fun setHttpOptions(video: Video) {
        if (viewModel.isEpisodeOnline() != true) return
        val source = viewModel.currentSource as AnimeHttpSource

        val headers = video.headers?.toMultimap()
            ?.mapValues { it.value.firstOrNull() ?: "" }
            ?.toMutableMap()
            ?: source.headers.toMultimap()
                .mapValues { it.value.firstOrNull() ?: "" }
                .toMutableMap()

        val httpHeaderString = headers.map {
            it.key + ": " + it.value.replace(",", "\\,")
        }.joinToString(",")

        MPVLib.setOptionString("http-header-fields", httpHeaderString)

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

    // TODO: exception java.util.ConcurrentModificationException:
    // at java.lang.Object java.util.ArrayList$Itr.next() (ArrayList.java:860)
    // at void eu.kanade.tachiyomi.ui.player.PlayerActivity.fileLoaded() (PlayerActivity.kt:1874)
    // at void eu.kanade.tachiyomi.ui.player.PlayerActivity.event(int) (PlayerActivity.kt:1566)
    // at void is.xyz.mpv.MPVLib.event(int) (MPVLib.java:86)
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
            currentVideoList?.getOrNull(selectedQuality)
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
                    .firstOrNull { player.sid == it.mpvId }
                selectedSub = mpvSub?.let {
                    subTracks.indexOfFirst { it.url == mpvSub.mpvId.toString() }
                }?.coerceAtLeast(0) ?: 0
            }
        }
        if (hadPreviousAudio) {
            audioTracks.getOrNull(selectedAudio)?.let { audio ->
                MPVLib.command(arrayOf("audio-add", audio.url, "select", audio.url))
            }
        } else {
            currentVideoList?.getOrNull(selectedQuality)
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
                    .firstOrNull { player.aid == it.mpvId }
                selectedAudio = mpvAudio?.let {
                    audioTracks.indexOfFirst { it.url == mpvAudio.mpvId.toString() }
                }?.coerceAtLeast(0) ?: 0
            }
        }

        viewModel.viewModelScope.launchUI {
            if (playerPreferences.adjustOrientationVideoDimensions().get()) {
                if ((player.videoW ?: 1) / (player.videoH ?: 1) >= 1) {
                    this@PlayerActivity.requestedOrientation = playerPreferences.defaultPlayerOrientationLandscape().get()
                    switchControlsOrientation(true)
                } else {
                    this@PlayerActivity.requestedOrientation = playerPreferences.defaultPlayerOrientationPortrait().get()
                    switchControlsOrientation(false)
                }
            }

            playerControls.updateDecoderButton()

            viewModel.mutableState.update {
                it.copy(isLoadingAdjacentEpisode = false)
            }
        }
        // aniSkip stuff
        waitingAniSkip = playerPreferences.waitingTimeAniSkip().get()
        runBlocking {
            aniSkipInterval = viewModel.aniSkipResponse(player.duration)
            playerControls.binding.playbackSeekbar.setStamps(aniSkipInterval)
        }
    }

    private val aniSkipEnable = playerPreferences.aniSkipEnabled().get()
    private val autoSkipAniSkip = playerPreferences.autoSkipAniSkip().get()
    private val netflixStyle = playerPreferences.enableNetflixStyleAniSkip().get()

    private var aniSkipInterval: List<Stamp>? = null
    private var waitingAniSkip = playerPreferences.waitingTimeAniSkip().get()

    private var skipType: SkipType? = null

    private suspend fun aniSkipStuff(position: Long) {
        if (!aniSkipEnable) return
        // if it doesn't find any interval it will show the +85 button
        if (aniSkipInterval == null) return

        skipType = aniSkipInterval?.firstOrNull { it.interval.startTime <= position && it.interval.endTime > position }?.skipType
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
            updateEpisodeText()
        }
    }

    // mpv events

    private fun mpvUpdateAspect(aspect: String, pan: String) {
        MPVLib.setOptionString("video-aspect-override", aspect)
        MPVLib.setOptionString("panscan", pan)
    }

    private fun eventPropertyUi(property: String, value: Long) {
        when (property) {
            "demuxer-cache-time" -> playerControls.updateBufferPosition(value.toInt())
            "time-pos" -> {
                playerControls.updatePlaybackPos(value.toInt())
                viewModel.viewModelScope.launchUI { aniSkipStuff(value) }
            }
            "duration" -> playerControls.updatePlaybackDuration(value.toInt())
        }
    }

    private val nextEpisodeRunnable = Runnable { switchEpisode(previous = false, autoPlay = true) }

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
            MPVLib.mpvEventId.MPV_EVENT_START_FILE -> viewModel.viewModelScope.launchUI {
                player.paused = false
                refreshUi()
                // Fixes a minor Ui bug but I have no idea why
                if (viewModel.isEpisodeOnline() != true) showLoadingIndicator(false)
            }
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
