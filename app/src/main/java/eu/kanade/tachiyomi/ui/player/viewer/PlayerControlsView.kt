package eu.kanade.tachiyomi.ui.player.viewer

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
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
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

        binding.pipBtn.setOnClickListener { activity.pip.start() }

        binding.pipBtn.isVisible = !playerPreferences.pipOnExit().get() && activity.pip.supportedAndEnabled

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

        binding.titleMainTxt.setOnClickListener {
            episodeListDialog()
        }

        binding.titleSecondaryTxt.setOnClickListener {
            episodeListDialog()
        }

        binding.episodeListBtn.setOnClickListener {
            episodeListDialog()
        }
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
            activity.player.paused!! -> animationHandler.removeCallbacks(fadeOutControlsRunnable)
            binding.unlockedView.isVisible -> showAndFadeControls()
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
            Pair("${HwDecType.HW.title} (${HwDecType.HW.mpvValue})", HwDecType.HW.mpvValue),
            Pair(HwDecType.SW.title, HwDecType.SW.mpvValue),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            items.add(
                index = 0,
                Pair("${HwDecType.HW_PLUS.title} (${HwDecType.HW_PLUS.mpvValue})", HwDecType.HW_PLUS.mpvValue),
            )
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

        var newSkipIntroLength = activity.viewModel.getAnimeSkipIntroLength()

        with(activity.HideBarsMaterialAlertDialogBuilder(context)) {
            setTitle(R.string.pref_intro_length)
            val binding = PrefSkipIntroLengthBinding.inflate(LayoutInflater.from(activity))

            with(binding.skipIntroColumn) {
                value = activity.viewModel.getAnimeSkipIntroLength()
                setOnValueChangedListener { _, _, newValue ->
                    newSkipIntroLength = newValue
                }
            }

            setView(binding.root)
            setNeutralButton(R.string.label_default) { _, _ ->
                activity.viewModel.setAnimeSkipIntroLength(playerPreferences.defaultIntroLength().get())
            }
            setPositiveButton(R.string.dialog_ok) { dialog, _ ->
                when (newSkipIntroLength) {
                    0 -> activity.viewModel.setAnimeSkipIntroLength(playerPreferences.defaultIntroLength().get())
                    activity.viewModel.getAnimeSkipIntroLength() -> dialog.cancel()
                    else -> activity.viewModel.setAnimeSkipIntroLength(newSkipIntroLength)
                }
            }
            setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
            setOnDismissListener { restore() }
            create()
            show()
        }
    }

    private fun episodeListDialog() {
    }
}
