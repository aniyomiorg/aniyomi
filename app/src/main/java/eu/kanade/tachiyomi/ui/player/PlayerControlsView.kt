package eu.kanade.tachiyomi.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
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
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PlayerControlsBinding
import eu.kanade.tachiyomi.databinding.PrefSkipIntroLengthBinding
import eu.kanade.tachiyomi.ui.player.setting.PlayerPreferences
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
    internal var shouldHideUiForSeek = false
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
            userIsOperatingSeekbar = true
            activity.initSeek()
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            val newPos = seekBar.progress
            if (playerPreferences.playerSmoothSeek().get()) activity.player.timePos = newPos else MPVLib.command(arrayOf("seek", newPos.toString(), "absolute+keyframes"))
            userIsOperatingSeekbar = false
            animationHandler.removeCallbacks(hideUiForSeekRunnable)
            animationHandler.postDelayed(hideUiForSeekRunnable, 500L)
            animationHandler.postDelayed(controlsViewRunnable, 3500L)
        }
    }

    private var showControls = false
    private var wasPausedBeforeSeeking = false

    private val nonSeekViewRunnable = Runnable {
        binding.topControlsGroup.isVisible = true
        binding.middleControlsGroup.isVisible = true
        binding.bottomControlsGroup.isVisible = true
    }

    private val hideUiForSeekRunnable = Runnable {
        shouldHideUiForSeek = false
        activity.player.paused = wasPausedBeforeSeeking
        if (showControls) {
            AnimationUtils.loadAnimation(activity, R.anim.player_fade_in).also { fadeAnimation ->
                binding.topControlsGroup.startAnimation(fadeAnimation)
                binding.topControlsGroup.isVisible = true

                binding.middleControlsGroup.startAnimation(fadeAnimation)
                binding.middleControlsGroup.isVisible = true

                binding.bottomControlsGroup.startAnimation(fadeAnimation)
                binding.bottomControlsGroup.isVisible = true
            }
            showControls = false
        } else {
            showControls = true

            animationHandler.removeCallbacks(controlsViewRunnable)
            animationHandler.postDelayed(controlsViewRunnable, 500L)
            animationHandler.removeCallbacks(nonSeekViewRunnable)
            animationHandler.postDelayed(nonSeekViewRunnable, 600L + resources.getInteger(R.integer.player_animation_duration).toLong())
        }
    }

    internal fun hideUiForSeek() {
        animationHandler.removeCallbacks(controlsViewRunnable)
        animationHandler.removeCallbacks(hideUiForSeekRunnable)

        if (!(binding.topControlsGroup.visibility == INVISIBLE && binding.middleControlsGroup.visibility == INVISIBLE && binding.bottomControlsGroup.visibility == INVISIBLE)) {
            wasPausedBeforeSeeking = activity.player.paused!!
            showControls = binding.controlsView.isVisible
            binding.topControlsGroup.visibility = INVISIBLE
            binding.middleControlsGroup.visibility = INVISIBLE
            binding.bottomControlsGroup.visibility = INVISIBLE
            activity.player.paused = true
            animationHandler.removeCallbacks(activity.volumeViewRunnable)
            animationHandler.removeCallbacks(activity.brightnessViewRunnable)
            animationHandler.removeCallbacks(activity.seekTextRunnable)
            binding.volumeView.isVisible = false
            binding.brightnessView.isVisible = false
            activity.binding.seekView.isVisible = false
            binding.seekBarGroup.isVisible = true
            binding.controlsView.isVisible = true
            shouldHideUiForSeek = true
        }

        val delay = if (activity.isDoubleTapSeeking) 1000L else 500L

        animationHandler.postDelayed(hideUiForSeekRunnable, delay)
    }

    private val playerPreferences: PlayerPreferences by injectLazy()

    private tailrec fun Context.getActivity(): PlayerActivity? = this as? PlayerActivity
        ?: (this as? ContextWrapper)?.baseContext?.getActivity()

    init {
        addView(binding.root)
    }

    override fun onViewAdded(child: View?) {
        binding.backArrowBtn.setOnClickListener { activity.onBackPressed() }

        // Lock and Unlock controls
        binding.lockBtn.setOnClickListener { lockControls(true) }
        binding.unlockBtn.setOnClickListener { lockControls(false) }

        // Long click controls
        binding.cycleSpeedBtn.setOnLongClickListener { pickSpeed(); true }
        binding.cycleDecoderBtn.setOnLongClickListener { pickDecoder(); true }

        binding.playbackSeekbar.setOnSeekBarChangeListener(seekBarChangeListener)

        binding.nextBtn.setOnClickListener { activity.switchEpisode(false) }
        binding.prevBtn.setOnClickListener { activity.switchEpisode(true) }

        binding.pipBtn.setOnClickListener { activity.startPiP() }

        binding.pipBtn.isVisible = !playerPreferences.pipOnExit().get() && activity.deviceSupportsPip

        binding.controlsSkipIntroBtn.setOnLongClickListener { skipIntroLengthDialog(); true }

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

        binding.toggleAutoplay.setOnCheckedChangeListener { _, isChecked ->
            activity.toggleAutoplay(isChecked)
        }
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

    internal fun lockControls(locked: Boolean) {
        activity.isLocked = locked
        toggleControls()
    }

    internal fun toggleControls() {
        if (activity.isLocked) {
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
        if (hide) {
            binding.controlsView.isVisible = false
            binding.lockedView.isVisible = false
        } else {
            showAndFadeControls()
        }
    }

    @SuppressLint("SetTextI18n")
    internal fun updatePlaybackPos(position: Int) {
        if (activity.player.duration != null) {
            if (playerPreferences.invertedPlaybackTxt().get()) {
                binding.playbackPositionTxt.text =
                    "-${Utils.prettyTime(activity.player.duration!! - position)}"
            } else if (playerPreferences.invertedDurationTxt().get()) {
                binding.playbackPositionTxt.text = Utils.prettyTime(position)
                binding.playbackDurationTxt.text =
                    "-${Utils.prettyTime(activity.player.duration!! - position)}"
            } else {
                binding.playbackPositionTxt.text = Utils.prettyTime(position)
            }
        }

        binding.playbackSeekbar.progress = position
        updateDecoderButton()
        updateSpeedButton()
    }

    @SuppressLint("SetTextI18n")
    internal fun updatePlaybackDuration(duration: Int) {
        if (!playerPreferences.invertedDurationTxt().get() && activity.player.duration != null) {
            binding.playbackDurationTxt.text = Utils.prettyTime(duration)
        }

        if (!userIsOperatingSeekbar) {
            binding.playbackSeekbar.max = duration
        }
    }

    internal fun updateBufferPosition(duration: Int) {
        binding.playbackSeekbar.secondaryProgress = duration
    }

    internal fun updateDecoderButton() {
        if (binding.cycleDecoderBtn.visibility != View.VISIBLE && binding.cycleDecoderBtn.visibility != View.VISIBLE) {
            return
        }
        binding.cycleDecoderBtn.text = when (activity.player.hwdecActive) {
            "mediacodec" -> "HW+"
            "no" -> "SW"
            else -> "HW"
        }
    }

    internal fun updateSpeedButton() {
        binding.cycleSpeedBtn.text = context.getString(R.string.ui_speed, activity.player.playbackSpeed)
        activity.player.playbackSpeed?.let { playerPreferences.playerSpeed().set(it.toFloat()) }
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

        AnimationUtils.loadAnimation(context, R.anim.player_fade_out).also { fadeAnimation ->
            view.startAnimation(fadeAnimation)
            view.visibility = View.GONE
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

    private fun fadeInView(view: View) {
        animationHandler.removeCallbacks(controlsViewRunnable)

        AnimationUtils.loadAnimation(context, R.anim.player_fade_in).also { fadeAnimation ->
            view.startAnimation(fadeAnimation)
            view.visibility = View.VISIBLE
        }

        binding.seekBarGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_enter_bottom))
        binding.topControlsGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_enter_top))
        binding.bottomRightControlsGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_enter_right))
        binding.bottomLeftControlsGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_enter_left))
        binding.middleControlsGroup.startAnimation(AnimationUtils.loadAnimation(context, R.anim.player_fade_in))
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

    private fun pickSpeed() {
        // TODO: replace this with SliderPickerDialog
        val picker = SpeedPickerDialog()

        val restore = pauseForDialog()
        speedPickerDialog(picker, R.string.title_speed_dialog) {
            updateSpeedButton()
            restore()
        }
    }

    private fun pickDecoder() {
        val restore = pauseForDialog()

        val items = mutableListOf(
            Pair("HW (mediacodec-copy)", "mediacodec-copy"),
            Pair("SW", "no"),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            items.add(0, Pair("HW+ (mediacodec)", "mediacodec"))
        }
        var hwdecActive = playerPreferences.standardHwDec().get()
        val selectedIndex = items.indexOfFirst { it.second == hwdecActive }
        with(activity.HideBarsMaterialAlertDialogBuilder(activity)) {
            setTitle(R.string.player_hwdec_dialog_title)
            setSingleChoiceItems(items.map { it.first }.toTypedArray(), selectedIndex) { _, idx ->
                hwdecActive = items[idx].second
            }
            setPositiveButton(R.string.dialog_ok) { _, _ ->
                playerPreferences.standardHwDec().set(hwdecActive)
                MPVLib.setPropertyString("hwdec", hwdecActive)
            }
            setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
            setOnDismissListener { restore() }
            create()
            show()
        }
    }

    private fun speedPickerDialog(
        picker: PickerDialog,
        @StringRes titleRes: Int,
        restoreState: StateRestoreCallback,
    ) {
        with(activity.HideBarsMaterialAlertDialogBuilder(context)) {
            setTitle(titleRes)
            setView(picker.buildView(LayoutInflater.from(context)))
            setPositiveButton(R.string.dialog_ok) { _, _ ->
                picker.number?.let {
                    playerPreferences.playerSpeed().set(it.toFloat())
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
            show()
        }
        picker.number = MPVLib.getPropertyDouble("speed")
    }

    private fun skipIntroLengthDialog() {
        val restore = pauseForDialog()

        var newSkipIntroLength = activity.presenter.getAnimeSkipIntroLength()

        with(activity.HideBarsMaterialAlertDialogBuilder(context)) {
            setTitle(R.string.pref_intro_length)
            val binding = PrefSkipIntroLengthBinding.inflate(LayoutInflater.from(activity))

            with(binding.skipIntroColumn) {
                value = activity.presenter.getAnimeSkipIntroLength()
                setOnValueChangedListener { _, _, newValue ->
                    newSkipIntroLength = newValue
                }
            }

            setView(binding.root)
            setNeutralButton(R.string.label_default) { _, _ ->
                activity.presenter.setAnimeSkipIntroLength(playerPreferences.defaultIntroLength().get())
            }
            setPositiveButton(R.string.dialog_ok) { dialog, _ ->
                when (newSkipIntroLength) {
                    0 -> activity.presenter.setAnimeSkipIntroLength(playerPreferences.defaultIntroLength().get())
                    activity.presenter.getAnimeSkipIntroLength() -> dialog.cancel()
                    else -> activity.presenter.setAnimeSkipIntroLength(newSkipIntroLength)
                }
            }
            setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
            setOnDismissListener { restore() }
            create()
            show()
        }
    }
}
