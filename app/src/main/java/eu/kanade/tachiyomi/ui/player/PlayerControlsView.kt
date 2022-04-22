package eu.kanade.tachiyomi.ui.player

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.PlayerControlsBinding
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.PickerDialog
import `is`.xyz.mpv.SpeedPickerDialog
import `is`.xyz.mpv.StateRestoreCallback
import `is`.xyz.mpv.Utils
import uy.kohesive.injekt.injectLazy

class PlayerControlsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    internal val binding: PlayerControlsBinding =
        PlayerControlsBinding.inflate(LayoutInflater.from(context), this, false)

    val activity: PlayerActivity = context.getActivity()!!

    private var userIsOperatingSeekbar = false

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser) {
                return
            }
            activity.player.timePos = progress
            updatePlaybackPos(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            userIsOperatingSeekbar = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            userIsOperatingSeekbar = false
        }
    }

    private val preferences: PreferencesHelper by injectLazy()

    private tailrec fun Context.getActivity(): PlayerActivity? = this as? PlayerActivity
        ?: (this as? ContextWrapper)?.baseContext?.getActivity()

    init {
        addView(binding.root)
    }

    override fun onViewAdded(child: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.pipBtn.isVisible = context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_PICTURE_IN_PICTURE,
            ) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        }
        binding.backArrowBtnLandscape.setOnClickListener { activity.finishAndRemoveTask() }
        binding.backArrowBtnPortrait.setOnClickListener { activity.finishAndRemoveTask() }

        binding.pipBtn.setOnClickListener { activity.startPiP() }

        // Lock and Unlock controls
        binding.lockBtn.setOnClickListener { activity.isLocked = true; toggleControls() }
        binding.unlockBtn.setOnClickListener { activity.isLocked = false; toggleControls() }

        // Cycle, Long click controls
        binding.cycleAudioBtnLandscape.setOnLongClickListener { pickAudio(); true }
        binding.cycleAudioBtnPortrait.setOnLongClickListener { pickAudio(); true }

        binding.cycleSpeedBtn.setOnLongClickListener { pickSpeed(); true }

        binding.cycleSubsBtnLandscape.setOnLongClickListener { pickSub(); true }
        binding.cycleSubsBtnPortrait.setOnLongClickListener { pickSub(); true }

        binding.playbackSeekbar.setOnSeekBarChangeListener(seekBarChangeListener)

        binding.nextBtn.setOnClickListener { activity.switchEpisode(false) }
        binding.prevBtn.setOnClickListener { activity.switchEpisode(true) }

        binding.settingsBtnPortrait.setOnClickListener { showSettings() }
    }

    private val animationHandler = Handler(Looper.getMainLooper())

    // Fade out Player controls
    private val controlsViewRunnable = Runnable {
        if (activity.isLocked) {
            fadeOutView(binding.lockedView)
        } else {
            fadeOutView(binding.controlsView)
        }
    }

    internal fun toggleControls() {
        if (activity.isLocked) {
            // Hide controls
            binding.controlsView.isVisible = false

            if (!binding.lockedView.isVisible && !activity.player.paused!!) {
                showAndFadeControls()
            } else if (!binding.lockedView.isVisible && activity.player.paused!!) {
                fadeInView(binding.lockedView)
            } else {
                fadeOutView(binding.lockedView)
            }
        } else {
            if (!binding.controlsView.isVisible && !activity.player.paused!!) {
                showAndFadeControls()
            } else if (!binding.controlsView.isVisible && activity.player.paused!!) {
                fadeInView(binding.controlsView)
            } else {
                fadeOutView(binding.controlsView)
            }

            binding.lockedView.isVisible = false
        }
    }

    internal fun hideControls(hide: Boolean) {
        animationHandler.removeCallbacks(controlsViewRunnable)
        if (hide) binding.controlsView.isVisible = false
        else showAndFadeControls()
    }

    internal fun updatePlaybackPos(position: Int) {
        binding.playbackPositionTxt.text = Utils.prettyTime(position)
        if (!userIsOperatingSeekbar) {
            binding.playbackSeekbar.progress = position
        }

        updateDecoderButton()
        updateSpeedButton()
    }

    internal fun updatePlaybackDuration(duration: Int) {
        binding.playbackDurationTxt.text = Utils.prettyTime(duration)
        if (!userIsOperatingSeekbar) {
            binding.playbackSeekbar.max = duration
        }
    }

    internal fun updateDecoderButton() {
        if (binding.cycleDecoderBtnLandscape.visibility != View.VISIBLE && binding.cycleDecoderBtnPortrait.visibility != View.VISIBLE) {
            return
        }
        binding.cycleDecoderBtnLandscape.text = if (activity.player.hwdecActive) "HW" else "SW"
        binding.cycleDecoderBtnPortrait.text = if (activity.player.hwdecActive) "HW" else "SW"
    }

    internal fun updateSpeedButton() {
        binding.cycleSpeedBtn.text = context.getString(R.string.ui_speed, activity.player.playbackSpeed)
        activity.player.playbackSpeed?.let { preferences.setPlayerSpeed(it.toFloat()) }
    }

    internal fun showAndFadeControls() {
        val itemView = if (!activity.isLocked) binding.controlsView else binding.lockedView
        if (!itemView.isVisible) fadeInView(itemView)
        itemView.visibility = View.VISIBLE
        resetControlsFade()
    }

    internal fun resetControlsFade() {
        val itemView = if (!activity.isLocked) binding.controlsView else binding.lockedView
        if (!itemView.isVisible) return
        animationHandler.removeCallbacks(controlsViewRunnable)
        if (userIsOperatingSeekbar) return
        animationHandler.postDelayed(controlsViewRunnable, 3500L)
    }

    private fun fadeOutView(view: View) {
        animationHandler.removeCallbacks(controlsViewRunnable)
        AnimationUtils.loadAnimation(context, R.anim.fade_out_medium).also { fadeAnimation ->
            view.startAnimation(fadeAnimation)
            view.visibility = View.GONE
        }
    }

    private fun fadeInView(view: View) {
        animationHandler.removeCallbacks(controlsViewRunnable)
        AnimationUtils.loadAnimation(context, R.anim.fade_in_short).also { fadeAnimation ->
            view.startAnimation(fadeAnimation)
            view.visibility = View.VISIBLE
        }
    }

    private fun pauseForDialog(): StateRestoreCallback {
        val wasPlayerPaused = activity.player.paused ?: true // default to not changing state
        activity.player.paused = true
        return {
            if (!wasPlayerPaused) {
                activity.player.paused = false
            }
        }
    }

    internal fun playPause() {
        when {
            activity.player.paused!! -> animationHandler.removeCallbacks(controlsViewRunnable)
            binding.controlsView.isVisible -> {
                showAndFadeControls()
            }
        }
    }

    private fun pickAudio() {
        if (activity.audioTracks.isEmpty()) return
        val restore = pauseForDialog()

        with(MaterialAlertDialogBuilder(context)) {
            setSingleChoiceItems(
                activity.audioTracks.map { it.lang }.toTypedArray(),
                activity.selectedAudio,
            ) { dialog, item ->
                if (item == activity.selectedAudio) return@setSingleChoiceItems
                if (item == 0) {
                    activity.selectedAudio = 0
                    activity.player.aid = -1
                    return@setSingleChoiceItems
                }
                activity.setAudio(item)
                dialog.dismiss()
            }
            setOnDismissListener { restore() }
            create().show()
        }
    }

    private fun pickSub() {
        if (activity.subTracks.isEmpty()) return
        val restore = pauseForDialog()

        with(MaterialAlertDialogBuilder(context)) {
            setSingleChoiceItems(
                activity.subTracks.map { it.lang }.toTypedArray(),
                activity.selectedSub,
            ) { dialog, item ->
                if (item == 0) {
                    activity.selectedSub = 0
                    activity.player.sid = -1
                    return@setSingleChoiceItems
                }
                activity.setSub(item)
                dialog.dismiss()
            }
            setOnDismissListener { restore() }
            create().show()
        }
    }

    private fun pickSpeed() {
        // TODO: replace this with SliderPickerDialog
        val picker = SpeedPickerDialog()

        val restore = pauseForDialog()
        speedPickerDialog(picker, R.string.title_speed_dialog) {
            updateSpeedButton()
            restore()
        }
    }

    private fun speedPickerDialog(
        picker: PickerDialog,
        @StringRes titleRes: Int,
        restoreState: StateRestoreCallback,
    ) {
        val dialog = with(MaterialAlertDialogBuilder(context)) {
            setTitle(titleRes)
            setView(picker.buildView(LayoutInflater.from(context)))
            setPositiveButton(R.string.dialog_ok) { _, _ ->
                picker.number?.let {
                    preferences.setPlayerSpeed(it.toFloat())
                    if (picker.isInteger()) {
                        MPVLib.setPropertyInt("speed", it.toInt())
                    } else {
                        MPVLib.setPropertyDouble("speed", it)
                    }
                }
            }
            setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
            setOnDismissListener { restoreState() }
            create()
        }

        picker.number = MPVLib.getPropertyDouble("speed")
        dialog.show()
    }

    private fun showSettings() {
        if (binding.settingsLayoutPortrait.isVisible) fadeOutView(binding.settingsLayoutPortrait)
        else fadeInView(binding.settingsLayoutPortrait)
    }
}
