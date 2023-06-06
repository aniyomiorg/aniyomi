package eu.kanade.tachiyomi.ui.player.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PlayerControlsBinding
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.viewer.components.PlayerDialogs
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.StateRestoreCallback
import `is`.xyz.mpv.Utils
import tachiyomi.core.util.lang.launchUI

class PlayerControlsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    internal val binding: PlayerControlsBinding =
        PlayerControlsBinding.inflate(LayoutInflater.from(context), this, false)

    private tailrec fun Context.getActivity(): PlayerActivity? = this as? PlayerActivity
        ?: (this as? ContextWrapper)?.baseContext?.getActivity()

    val activity: PlayerActivity = context.getActivity()!!

    private val playerPreferences = activity.playerPreferences

    private val playerDialogs = PlayerDialogs(activity)

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
        binding.cycleSpeedBtn.setOnLongClickListener { playerDialogs.speedPickerDialog(pauseForDialog()); true }
        binding.cycleDecoderBtn.setOnLongClickListener { playerDialogs.decoderDialog(pauseForDialog()); true }

        binding.playbackSeekbar.setOnSeekBarChangeListener(seekBarChangeListener)

        binding.nextBtn.setOnClickListener { activity.switchEpisode(false) }
        binding.prevBtn.setOnClickListener { activity.switchEpisode(true) }

        binding.pipBtn.setOnClickListener { activity.pip.start() }

        binding.pipBtn.isVisible = !playerPreferences.pipOnExit().get() && activity.pip.supportedAndEnabled

        binding.controlsSkipIntroBtn.setOnLongClickListener { playerDialogs.skipIntroDialog(pauseForDialog()); true }

        binding.playbackPositionBtn.setOnClickListener {
            if (activity.player.timePos != null && activity.player.duration != null) {
                playerPreferences.invertedDurationTxt().set(false)
                playerPreferences.invertedPlaybackTxt().set(!playerPreferences.invertedPlaybackTxt().get())
                updatePlaybackPos(activity.player.timePos!!)
                updatePlaybackDuration(activity.player.duration!!)
            }
        }

        binding.playbackDurationBtn.setOnClickListener {
            if (activity.player.timePos != null && activity.player.duration != null) {
                playerPreferences.invertedPlaybackTxt().set(false)
                playerPreferences.invertedDurationTxt().set(!playerPreferences.invertedDurationTxt().get())
                updatePlaybackPos(activity.player.timePos!!)
                updatePlaybackDuration(activity.player.duration!!)
            }
        }

        binding.toggleAutoplay.setOnCheckedChangeListener { _, isChecked -> activity.toggleAutoplay(isChecked) }

        binding.titleMainTxt.setOnClickListener { playerDialogs.episodeListDialog(pauseForDialog()) }

        binding.titleSecondaryTxt.setOnClickListener { playerDialogs.episodeListDialog(pauseForDialog()) }

        binding.episodeListBtn.setOnClickListener { playerDialogs.episodeListDialog(pauseForDialog()) }
    }

    private fun pauseForDialog(): StateRestoreCallback {
        val wasPlayerPaused = activity.player.paused ?: true // default to not changing state
        activity.player.paused = true
        return {
            if (!wasPlayerPaused) {
                activity.player.paused = false
                activity.viewModel.viewModelScope.launchUI { activity.refreshUi() }
            }
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
        activity.player.paused = wasPausedBeforeSeeking
        if (showControls) {
            AnimationUtils.loadAnimation(activity, R.anim.player_fade_in).also { fadeAnimation ->
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
            wasPausedBeforeSeeking = activity.player.paused!!
            showControls = binding.unlockedView.isVisible
            binding.topControlsGroup.visibility = View.INVISIBLE
            binding.middleControlsGroup.visibility = View.INVISIBLE
            binding.bottomControlsGroup.visibility = View.INVISIBLE
            activity.player.paused = true
            animationHandler.removeCallbacks(activity.volumeViewRunnable)
            animationHandler.removeCallbacks(activity.brightnessViewRunnable)
            animationHandler.removeCallbacks(activity.seekTextRunnable)
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
    private val fadeOutControlsRunnable = Runnable { fadeOutControls() }

    internal fun lockControls(locked: Boolean) {
        SeekState.mode = if (locked) SeekState.LOCKED else SeekState.NONE
        val itemView = if (locked) binding.unlockedView else binding.lockedView
        itemView.visibility = View.GONE
        showAndFadeControls()
    }

    internal fun toggleControls(isTapped: Boolean = false) {
        val isControlsVisible = binding.lockedView.isVisible || binding.unlockedView.isVisible
        if (!isControlsVisible && !activity.player.paused!!) {
            showAndFadeControls()
        } else if (!isControlsVisible && activity.player.paused!!) {
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
        val duration = activity.player.duration
        val invertedPlayback = playerPreferences.invertedPlaybackTxt().get()
        val invertedDuration = playerPreferences.invertedDurationTxt().get()

        if (duration != null) {
            if (invertedPlayback) {
                binding.playbackPositionTxt.text = "-${Utils.prettyTime(duration - position)}"
            } else if (invertedDuration) {
                binding.playbackPositionTxt.text = Utils.prettyTime(position)
                binding.playbackDurationTxt.text = "-${Utils.prettyTime(duration - position)}"
            } else {
                binding.playbackPositionTxt.text = Utils.prettyTime(position)
            }
            activity.viewModel.onSecondReached(position, duration)
        }

        binding.playbackSeekbar.progress = position
        updateSpeedButton()
    }

    @SuppressLint("SetTextI18n")
    internal fun updatePlaybackDuration(duration: Int) {
        if (!playerPreferences.invertedDurationTxt().get() && activity.player.duration != null) {
            binding.playbackDurationTxt.text = Utils.prettyTime(duration)
        }

        if (SeekState.mode != SeekState.SEEKBAR) {
            binding.playbackSeekbar.max = duration
        }
    }

    internal fun updateBufferPosition(duration: Int) {
        binding.playbackSeekbar.secondaryProgress = duration
    }

    internal fun updateSpeedButton() {
        binding.cycleSpeedBtn.text = context.getString(R.string.ui_speed, activity.player.playbackSpeed)
        activity.player.playbackSpeed?.let { playerPreferences.playerSpeed().set(it.toFloat()) }
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
        when {
            activity.player.paused!! -> animationHandler.removeCallbacks(fadeOutControlsRunnable)
            binding.unlockedView.isVisible -> showAndFadeControls()
        }
    }

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser) {
                return
            }
            hideUiForSeek()
            MPVLib.command(arrayOf("seek", progress.toString(), "absolute+keyframes"))
            updatePlaybackPos(progress)

            val duration = activity.player.duration ?: 0
            if (duration == 0 || activity.initialSeek < 0) {
                return
            }
            val newDiff = activity.player.timePos!! - activity.initialSeek

            val diffText = Utils.prettyTime(newDiff, true)
            activity.binding.seekText.text = activity.getString(R.string.ui_seek_distance, Utils.prettyTime(activity.player.timePos!!), diffText)
            activity.showGestureView("seek")
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            SeekState.mode = SeekState.SEEKBAR
            activity.initSeek()
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            val newPos = seekBar.progress
            if (playerPreferences.playerSmoothSeek().get()) activity.player.timePos = newPos else MPVLib.command(arrayOf("seek", newPos.toString(), "absolute+keyframes"))
            SeekState.mode = SeekState.NONE
            animationHandler.removeCallbacks(hideUiForSeekRunnable)
            animationHandler.postDelayed(hideUiForSeekRunnable, 500L)
            animationHandler.postDelayed(fadeOutControlsRunnable, 3500L)
        }
    }
}
