package eu.kanade.tachiyomi.ui.player.viewer

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
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import tachiyomi.core.i18n.stringResource
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



    init {
        addView(binding.root)
    }

    override fun onViewAdded(child: View?) {

    }

    private var showControls = false
    private var wasPausedBeforeSeeking = false

    private val nonSeekViewRunnable = Runnable {
    }

    private val hideUiForSeekRunnable = Runnable {
        SeekState.mode = SeekState.NONE
        player.paused = wasPausedBeforeSeeking
        if (showControls) {
            showControls = false
        } else {
            showControls = true

            animationHandler.removeCallbacks(fadeOutControlsRunnable)
            animationHandler.postDelayed(fadeOutControlsRunnable, 500L)
            animationHandler.removeCallbacks(nonSeekViewRunnable)
            animationHandler.postDelayed(
                nonSeekViewRunnable,
                600L + resources.getInteger(R.integer.player_animation_duration).toLong(),
            )
        }
    }

    internal fun hideUiForSeek() {
        animationHandler.removeCallbacks(fadeOutControlsRunnable)
        animationHandler.removeCallbacks(hideUiForSeekRunnable)

            wasPausedBeforeSeeking = player.paused!!
            player.paused = true
            animationHandler.removeCallbacks(volumeViewRunnable)
            animationHandler.removeCallbacks(brightnessViewRunnable)
            animationHandler.removeCallbacks(seekTextRunnable)
            binding.volumeView.visibility = View.GONE
            binding.brightnessView.visibility = View.GONE
            activity.binding.seekView.visibility = View.GONE
            SeekState.mode = SeekState.SCROLL

        val delay = if (SeekState.mode == SeekState.DOUBLE_TAP) 1000L else 500L

        animationHandler.postDelayed(hideUiForSeekRunnable, delay)
    }

    private val animationHandler = Handler(Looper.getMainLooper())

    // Fade out Player controls
    internal val fadeOutControlsRunnable = Runnable { fadeOutControls() }

    internal fun lockControls(locked: Boolean) {
    }

    internal fun toggleControls(isTapped: Boolean = false) {
    }

    internal fun hideControls(hide: Boolean) {
        animationHandler.removeCallbacks(fadeOutControlsRunnable)
    }

    internal fun showAndFadeControls() {
        resetControlsFade()
    }

    internal fun resetControlsFade() {
        animationHandler.removeCallbacks(fadeOutControlsRunnable)
        if (SeekState.mode == SeekState.SEEKBAR) return
        animationHandler.postDelayed(fadeOutControlsRunnable, 3500L)
    }

    private fun fadeOutControls() {
        animationHandler.removeCallbacks(fadeOutControlsRunnable)
        showControls = false
    }

    private fun fadeInControls() {
        animationHandler.removeCallbacks(fadeOutControlsRunnable)
    }

    internal fun playPause() {
        player.cyclePause()
        when {
            player.paused!! -> animationHandler.removeCallbacks(fadeOutControlsRunnable)
        }
    }

    // Fade out seek text
    private val seekTextRunnable = Runnable {
        activity.binding.seekView.visibility = View.GONE
    }

    // Slide out Volume Bar
    private val volumeViewRunnable = Runnable {
        AnimationUtils.loadAnimation(context, R.anim.player_exit_left).also { slideAnimation ->
            if (SeekState.mode != SeekState.SCROLL) {
                binding.volumeView.startAnimation(
                    slideAnimation,
                )
            }
            binding.volumeView.visibility = View.GONE
        }
    }

    // Slide out Brightness Bar
    private val brightnessViewRunnable = Runnable {
        AnimationUtils.loadAnimation(context, R.anim.player_exit_right).also { slideAnimation ->
            if (SeekState.mode != SeekState.SCROLL) {
                binding.brightnessView.startAnimation(
                    slideAnimation,
                )
            }
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
                if (!itemView.isVisible) {
                    itemView.startAnimation(
                        AnimationUtils.loadAnimation(context, R.anim.player_enter_left),
                    )
                }
            }
            "brightness" -> {
                callback = brightnessViewRunnable
                itemView = binding.brightnessView
                delay = 750L
                if (!itemView.isVisible) {
                    itemView.startAnimation(
                        AnimationUtils.loadAnimation(context, R.anim.player_enter_right),
                    )
                }
            }
            else -> return
        }

        animationHandler.removeCallbacks(callback)
        itemView.visibility = View.VISIBLE
        animationHandler.postDelayed(callback, delay)
    }

    internal fun showSeekText(position: Int, difference: Int) {
        hideUiForSeek()

        val diffText = Utils.prettyTime(difference, true)
        activity.binding.seekText.text = activity.getString(
            R.string.ui_seek_distance,
            Utils.prettyTime(position),
            diffText,
        )
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
