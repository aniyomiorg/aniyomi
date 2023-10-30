package eu.kanade.tachiyomi.ui.player.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PlayerControlsBinding
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.viewer.components.Seekbar
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import tachiyomi.core.util.lang.withUIContext
import kotlin.math.abs

class PlayerControlsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    internal val binding: PlayerControlsBinding =
        PlayerControlsBinding.inflate(LayoutInflater.from(context), this, false)

    private tailrec fun Context.getActivity(): PlayerActivity? = this as? PlayerActivity
        ?: (this as? ContextWrapper)?.baseContext?.getActivity()

    private val activity: PlayerActivity = context.getActivity()!!

    private val playerPreferences = activity.playerPreferences

    private val player get() = activity.player

    val seekbar: Seekbar = Seekbar(
        view = binding.playbackSeekbar,
        onValueChange = ::onValueChange,
        onValueChangeFinished = ::onValueChangeFinished,
    )

    private fun onValueChange(value: Float, wasSeeking: Boolean) {
        if (!wasSeeking) {
            SeekState.mode = SeekState.SEEKBAR
            activity.initSeek()
        }

        MPVLib.command(arrayOf("seek", value.toInt().toString(), "absolute+keyframes"))

        val duration = player.duration ?: 0
        if (duration == 0 || activity.initialSeek < 0) {
            return
        }

        val difference = value.toInt() - activity.initialSeek

        showSeekText(value.toInt(), difference)
    }

    private fun onValueChangeFinished(value: Float) {
        if (SeekState.mode == SeekState.SEEKBAR) {
            if (playerPreferences.playerSmoothSeek().get()) player.timePos = value.toInt() else MPVLib.command(arrayOf("seek", value.toInt().toString(), "absolute+keyframes"))
            SeekState.mode = SeekState.NONE
            animationHandler.removeCallbacks(hideUiForSeekRunnable)
            animationHandler.removeCallbacks(fadeOutControlsRunnable)
            animationHandler.postDelayed(hideUiForSeekRunnable, 500L)
            animationHandler.postDelayed(fadeOutControlsRunnable, 3500L)
        } else {
            MPVLib.command(arrayOf("seek", value.toInt().toString(), "absolute+keyframes"))
        }
    }

    init {
        addView(binding.root)
    }

    @Suppress("DEPRECATION")
    override fun onViewAdded(child: View?) {
        binding.backArrowBtn.setOnClickListener { activity.onBackPressed() }

        // Lock and Unlock controls
        binding.lockBtn.setOnClickListener { lockControls(true) }
        binding.unlockBtn.setOnClickListener { lockControls(false) }

        // Long click controls
        binding.cycleSpeedBtn.setOnLongClickListener { activity.viewModel.showSpeedPicker(); true }

        binding.prevBtn.setOnClickListener { switchEpisode(previous = true) }
        binding.playBtn.setOnClickListener { playPause() }
        binding.nextBtn.setOnClickListener { switchEpisode(previous = false) }

        binding.pipBtn.setOnClickListener { activity.pip.start() }

        binding.pipBtn.isVisible = !playerPreferences.pipOnExit().get() && activity.pip.supportedAndEnabled

        binding.controlsSkipIntroBtn.setOnLongClickListener { activity.viewModel.showSkipIntroLength(); true }

        binding.playbackPositionBtn.setOnClickListener {
            if (player.timePos != null && player.duration != null) {
                playerPreferences.invertedDurationTxt().set(false)
                playerPreferences.invertedPlaybackTxt().set(!playerPreferences.invertedPlaybackTxt().get())
                updatePlaybackPos(player.timePos!!)
                updatePlaybackDuration(player.duration!!)
            }
        }

        binding.playbackDurationBtn.setOnClickListener {
            if (player.timePos != null && player.duration != null) {
                playerPreferences.invertedPlaybackTxt().set(false)
                playerPreferences.invertedDurationTxt().set(!playerPreferences.invertedDurationTxt().get())
                updatePlaybackPos(player.timePos!!)
                updatePlaybackDuration(player.duration!!)
            }
        }

        binding.toggleAutoplay.setOnCheckedChangeListener { _, isChecked -> toggleAutoplay(isChecked) }

        binding.cycleViewModeBtn.setOnClickListener { cycleViewMode() }

        binding.settingsBtn.setOnClickListener { activity.viewModel.showPlayerSettings() }

        binding.tracksBtn.setOnClickListener { activity.viewModel.showTracksCatalog() }

        binding.chaptersBtn.setOnClickListener { activity.viewModel.showVideoChapters() }

        binding.titleMainTxt.setOnClickListener { activity.viewModel.showEpisodeList() }

        binding.titleSecondaryTxt.setOnClickListener { activity.viewModel.showEpisodeList() }

        binding.episodeListBtn.setOnClickListener { activity.viewModel.showEpisodeList() }
    }

    private fun switchEpisode(previous: Boolean) {
        return activity.changeEpisode(activity.viewModel.getAdjacentEpisodeId(previous = previous))
    }

    internal suspend fun updateEpisodeText() {
        val viewModel = activity.viewModel
        val skipIntroText = activity.getString(R.string.player_controls_skip_intro_text, viewModel.getAnimeSkipIntroLength())
        withUIContext {
            binding.titleMainTxt.text = viewModel.currentAnime?.title
            binding.titleSecondaryTxt.text = viewModel.currentEpisode?.name
            binding.controlsSkipIntroBtn.text = skipIntroText
        }
    }

    internal suspend fun updatePlaylistButtons() {
        val viewModel = activity.viewModel
        val plCount = viewModel.currentPlaylist.size
        val plPos = viewModel.getCurrentEpisodeIndex()

        val grey = ContextCompat.getColor(context, R.color.tint_disabled)
        val white = ContextCompat.getColor(context, R.color.tint_normal)
        withUIContext {
            with(binding.prevBtn) {
                this.imageTintList = ColorStateList.valueOf(if (plPos == 0) grey else white)
                this.isClickable = plPos != 0
            }
            with(binding.nextBtn) {
                this.imageTintList =
                    ColorStateList.valueOf(if (plPos == plCount - 1) grey else white)
                this.isClickable = plPos != plCount - 1
            }
        }
    }

    internal suspend fun updateSpeedButton() {
        withUIContext {
            binding.cycleSpeedBtn.text = context.getString(R.string.ui_speed, player.playbackSpeed)
            player.playbackSpeed?.let { playerPreferences.playerSpeed().set(it.toFloat()) }
        }
    }

    private var showControls = false
    private var wasPausedBeforeSeeking = false

    private val nonSeekViewRunnable = Runnable {
        binding.topControlsGroup.visibility = View.VISIBLE
        binding.middleControlsGroup.visibility = View.VISIBLE
        binding.bottomControlsGroup.visibility = View.VISIBLE
    }

    private val hideUiForSeekRunnable = Runnable {
        SeekState.mode = SeekState.NONE
        player.paused = wasPausedBeforeSeeking
        if (showControls) {
            AnimationUtils.loadAnimation(context, R.anim.player_fade_in).also { fadeAnimation ->
                binding.topControlsGroup.startAnimation(fadeAnimation)
                binding.topControlsGroup.visibility = View.VISIBLE

                binding.middleControlsGroup.startAnimation(fadeAnimation)
                binding.middleControlsGroup.visibility = View.VISIBLE

                binding.bottomControlsGroup.startAnimation(fadeAnimation)
                binding.bottomControlsGroup.visibility = View.VISIBLE
            }
            showControls = false
        } else {
            showControls = true

            animationHandler.removeCallbacks(fadeOutControlsRunnable)
            animationHandler.postDelayed(fadeOutControlsRunnable, 500L)
            animationHandler.removeCallbacks(nonSeekViewRunnable)
            animationHandler.postDelayed(nonSeekViewRunnable, 600L + resources.getInteger(R.integer.player_animation_duration).toLong())
        }
    }

    internal fun hideUiForSeek() {
        animationHandler.removeCallbacks(fadeOutControlsRunnable)
        animationHandler.removeCallbacks(hideUiForSeekRunnable)

        if (!(binding.topControlsGroup.visibility == View.INVISIBLE && binding.middleControlsGroup.visibility == INVISIBLE && binding.bottomControlsGroup.visibility == INVISIBLE)) {
            wasPausedBeforeSeeking = player.paused!!
            showControls = binding.unlockedView.isVisible
            binding.topControlsGroup.visibility = View.INVISIBLE
            binding.middleControlsGroup.visibility = View.INVISIBLE
            binding.bottomControlsGroup.visibility = View.INVISIBLE
            player.paused = true
            animationHandler.removeCallbacks(volumeViewRunnable)
            animationHandler.removeCallbacks(brightnessViewRunnable)
            animationHandler.removeCallbacks(seekTextRunnable)
            binding.volumeView.visibility = View.GONE
            binding.brightnessView.visibility = View.GONE
            activity.binding.seekView.visibility = View.GONE
            binding.seekBarGroup.visibility = View.VISIBLE
            binding.unlockedView.visibility = View.VISIBLE
            SeekState.mode = SeekState.SCROLL
        }

        val delay = if (SeekState.mode == SeekState.DOUBLE_TAP) 1000L else 500L

        animationHandler.postDelayed(hideUiForSeekRunnable, delay)
    }

    private val animationHandler = Handler(Looper.getMainLooper())

    // Fade out Player controls
    internal val fadeOutControlsRunnable = Runnable { fadeOutControls() }

    internal fun lockControls(locked: Boolean) {
        SeekState.mode = if (locked) SeekState.LOCKED else SeekState.NONE
        val itemView = if (locked) binding.unlockedView else binding.lockedView
        itemView.visibility = View.GONE
        showAndFadeControls()
    }

    internal fun toggleControls(isTapped: Boolean = false) {
        val isControlsVisible = binding.lockedView.isVisible || binding.unlockedView.isVisible
        if (!isControlsVisible && !player.paused!!) {
            showAndFadeControls()
        } else if (!isControlsVisible && player.paused!!) {
            fadeInControls()
        } else if (isTapped) {
            fadeOutControls()
        }
    }

    internal fun hideControls(hide: Boolean) {
        animationHandler.removeCallbacks(fadeOutControlsRunnable)
        if (hide) {
            binding.unlockedView.visibility = View.GONE
            binding.lockedView.visibility = View.GONE
        } else {
            showAndFadeControls()
        }
    }

    @SuppressLint("SetTextI18n")
    internal fun updatePlaybackPos(position: Int) {
        val duration = player.duration
        val invertedPlayback = playerPreferences.invertedPlaybackTxt().get()
        val invertedDuration = playerPreferences.invertedDurationTxt().get()

        if (duration != null) {
            if (invertedPlayback) {
                binding.playbackPositionBtn.text = "-${Utils.prettyTime(duration - position)}"
            } else if (invertedDuration) {
                binding.playbackPositionBtn.text = Utils.prettyTime(position)
                binding.playbackDurationBtn.text = "-${Utils.prettyTime(duration - position)}"
            } else {
                binding.playbackPositionBtn.text = Utils.prettyTime(position)
            }
            activity.viewModel.onSecondReached(position, duration)
        }
        seekbar.updateSeekbar(value = position.toFloat())
    }

    @SuppressLint("SetTextI18n")
    internal fun updatePlaybackDuration(duration: Int) {
        if (!playerPreferences.invertedDurationTxt().get() && player.duration != null) {
            binding.playbackDurationBtn.text = Utils.prettyTime(duration)
        }

        seekbar.updateSeekbar(duration = duration.toFloat())
    }

    internal fun updateBufferPosition(bufferPosition: Int) {
        seekbar.updateSeekbar(readAheadValue = bufferPosition.toFloat())
    }

    internal fun showAndFadeControls() {
        val itemView = if (SeekState.mode == SeekState.LOCKED) binding.lockedView else binding.unlockedView
        if (!itemView.isVisible) fadeInControls()
        itemView.visibility = View.VISIBLE
        resetControlsFade()
    }

    internal fun resetControlsFade() {
        val itemView = if (SeekState.mode == SeekState.LOCKED) binding.lockedView else binding.unlockedView
        if (!itemView.isVisible) return
        animationHandler.removeCallbacks(fadeOutControlsRunnable)
        if (SeekState.mode == SeekState.SEEKBAR) return
        animationHandler.postDelayed(fadeOutControlsRunnable, 3500L)
    }

    private fun fadeOutControls() {
        animationHandler.removeCallbacks(fadeOutControlsRunnable)

        AnimationUtils.loadAnimation(context, R.anim.player_fade_out).also { fadeAnimation ->
            val itemView = if (SeekState.mode == SeekState.LOCKED) binding.lockedView else binding.unlockedView
            itemView.startAnimation(fadeAnimation)
            itemView.visibility = View.GONE
        }

        binding.seekBarGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_exit_bottom))
        if (!showControls) {
            binding.topControlsGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_exit_top))
            binding.bottomRightControlsGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_exit_right))
            binding.bottomLeftControlsGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_exit_left))
            binding.middleControlsGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_fade_out))
        }
        showControls = false
    }

    private fun fadeInControls() {
        animationHandler.removeCallbacks(fadeOutControlsRunnable)

        AnimationUtils.loadAnimation(context, R.anim.player_fade_in).also { fadeAnimation ->
            val itemView = if (SeekState.mode == SeekState.LOCKED) binding.lockedView else binding.unlockedView
            itemView.startAnimation(fadeAnimation)
            itemView.visibility = View.VISIBLE
        }

        binding.seekBarGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_enter_bottom))
        binding.topControlsGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_enter_top))
        binding.bottomRightControlsGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_enter_right))
        binding.bottomLeftControlsGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_enter_left))
        binding.middleControlsGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_fade_in))
    }

    internal fun playPause() {
        player.cyclePause()
        when {
            player.paused!! -> animationHandler.removeCallbacks(fadeOutControlsRunnable)
            binding.unlockedView.isVisible -> showAndFadeControls()
        }
    }

    // Fade out Player information text
    private val playerInformationRunnable = Runnable {
        AnimationUtils.loadAnimation(context, R.anim.player_fade_out).also { fadeAnimation ->
            binding.playerInformation.startAnimation(fadeAnimation)
            binding.playerInformation.visibility = View.GONE
        }
    }

    private fun cycleViewMode() {
        AspectState.mode = when (AspectState.mode) {
            AspectState.FIT -> AspectState.CROP
            AspectState.CROP -> AspectState.STRETCH
            else -> AspectState.FIT
        }
        setViewMode(showText = true)
    }

    internal fun setViewMode(showText: Boolean) {
        binding.playerInformation.text = activity.getString(AspectState.mode.stringRes)
        var aspect = "-1"
        var pan = "1.0"
        when (AspectState.mode) {
            AspectState.CROP -> {
                pan = "1.0"
            }
            AspectState.FIT -> {
                pan = "0.0"
            }
            AspectState.STRETCH -> {
                aspect = "${activity.deviceWidth}/${activity.deviceHeight}"
                pan = "0.0"
            }
            AspectState.CUSTOM -> {
                aspect = MPVLib.getPropertyString("video-aspect-override")
            }
        }

        mpvUpdateAspect(aspect = aspect, pan = pan)
        playerPreferences.playerViewMode().set(AspectState.mode.index)

        if (showText) {
            animationHandler.removeCallbacks(playerInformationRunnable)
            binding.playerInformation.visibility = View.VISIBLE
            animationHandler.postDelayed(playerInformationRunnable, 1000L)
        }
    }

    private fun mpvUpdateAspect(aspect: String, pan: String) {
        MPVLib.setPropertyString("video-aspect-override", aspect)
        MPVLib.setPropertyString("panscan", pan)
    }

    internal fun toggleAutoplay(isAutoplay: Boolean) {
        binding.toggleAutoplay.isChecked = isAutoplay
        binding.toggleAutoplay.thumbDrawable = if (isAutoplay) {
            ContextCompat.getDrawable(context, R.drawable.ic_play_circle_filled_24)
        } else {
            ContextCompat.getDrawable(context, R.drawable.ic_pause_circle_filled_24)
        }

        if (isAutoplay) {
            binding.playerInformation.text = activity.getString(R.string.enable_auto_play)
        } else {
            binding.playerInformation.text = activity.getString(R.string.disable_auto_play)
        }

        if (!playerPreferences.autoplayEnabled().get() == isAutoplay) {
            animationHandler.removeCallbacks(playerInformationRunnable)
            binding.playerInformation.visibility = View.VISIBLE
            animationHandler.postDelayed(playerInformationRunnable, 1000L)
        }
        playerPreferences.autoplayEnabled().set(isAutoplay)
    }

    // Fade out seek text
    private val seekTextRunnable = Runnable {
        activity.binding.seekView.visibility = View.GONE
    }

    // Slide out Volume Bar
    private val volumeViewRunnable = Runnable {
        AnimationUtils.loadAnimation(context, R.anim.player_exit_left).also { slideAnimation ->
            if (SeekState.mode != SeekState.SCROLL) binding.volumeView.startAnimation(slideAnimation)
            binding.volumeView.visibility = View.GONE
        }
    }

    // Slide out Brightness Bar
    private val brightnessViewRunnable = Runnable {
        AnimationUtils.loadAnimation(context, R.anim.player_exit_right).also { slideAnimation ->
            if (SeekState.mode != SeekState.SCROLL) binding.brightnessView.startAnimation(slideAnimation)
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
                itemView = activity.binding.seekView
                delay = 0L
            }
            "volume" -> {
                callback = volumeViewRunnable
                itemView = binding.volumeView
                delay = 750L
                if (!itemView.isVisible) itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_enter_left))
            }
            "brightness" -> {
                callback = brightnessViewRunnable
                itemView = binding.brightnessView
                delay = 750L
                if (!itemView.isVisible) itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_enter_right))
            }
            else -> return
        }

        animationHandler.removeCallbacks(callback)
        itemView.visibility = View.VISIBLE
        animationHandler.postDelayed(callback, delay)
    }

    internal fun showSeekText(position: Int, difference: Int) {
        hideUiForSeek()
        updatePlaybackPos(position)

        val diffText = Utils.prettyTime(difference, true)
        activity.binding.seekText.text = activity.getString(R.string.ui_seek_distance, Utils.prettyTime(position), diffText)
        showGestureView("seek")
    }

    internal fun showVolumeBar(showBar: Boolean, volume: Int) {
        binding.volumeText.text = volume.toString()
        binding.volumeBar.progress = volume
        if (volume == 0) {
            binding.volumeImg.setImageResource(R.drawable.ic_volume_off_24dp)
        } else {
            binding.volumeImg.setImageResource(R.drawable.ic_volume_on_20dp)
        }
        if (showBar) showGestureView("volume")
    }

    internal fun showBrightnessBar(showBar: Boolean, brightness: Int) {
        binding.brightnessText.text = brightness.toString()
        binding.brightnessBar.progress = abs(brightness)
        if (brightness >= 0) {
            binding.brightnessImg.setImageResource(R.drawable.ic_brightness_positive_20dp)
            binding.brightnessBar.max = 100
            binding.brightnessBar.secondaryProgress = 100
        } else {
            binding.brightnessImg.setImageResource(R.drawable.ic_brightness_negative_20dp)
            binding.brightnessBar.max = 75
            binding.brightnessBar.secondaryProgress = 75
        }
        if (showBar) showGestureView("brightness")
    }
}
