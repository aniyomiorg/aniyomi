package eu.kanade.tachiyomi.ui.player.settings.sheets

import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.databinding.PlayerOptionsSheetBinding
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.widget.sheet.PlayerBottomSheetDialog

/**
 * Sheet to show when overflow button in player is clicked.
 *
 * @param activity the instance of the PlayerActivity in use.
 */
class PlayerOptionsSheet(
    private val activity: PlayerActivity,
) : PlayerBottomSheetDialog(activity) {

    private lateinit var binding: PlayerOptionsSheetBinding
    private var wasPaused: Boolean? = null

    override fun createView(inflater: LayoutInflater): View {
        wasPaused = activity.player.paused
        activity.player.paused = true
        binding = PlayerOptionsSheetBinding.inflate(activity.layoutInflater, null, false)

        val gestureVolumeBrightness = activity.playerPreferences.gestureVolumeBrightness()
        val gestureHorizontalSeek = activity.playerPreferences.gestureHorizontalSeek()
        binding.toggleVolumeBrightnessGestures.isChecked = gestureVolumeBrightness.get()
        binding.toggleVolumeBrightnessGestures.setOnCheckedChangeListener { _, newValue -> gestureVolumeBrightness.set(newValue) }
        binding.toggleHorizontalSeekGesture.isChecked = gestureHorizontalSeek.get()
        binding.toggleHorizontalSeekGesture.setOnCheckedChangeListener { _, newValue -> gestureHorizontalSeek.set(newValue) }

        binding.toggleStats.isChecked = activity.stats
        binding.toggleStats.setOnCheckedChangeListener(toggleStats)

        binding.statsPage.isVisible = activity.stats
        binding.statsPage.setSelection(activity.statsPage)
        binding.statsPage.onItemSelectedListener = setStatsPage

        return binding.root
    }

    private val toggleStats = { _: CompoundButton, newValue: Boolean ->
        binding.statsPage.isVisible = newValue
        activity.toggleStats()
    }

    private val setStatsPage = { page: Int ->
        activity.statsPage = page
    }

    override fun dismiss() {
        activity.playerControls.showAndFadeControls()
        wasPaused?.let { activity.player.paused = it }
        super.dismiss()
        activity.refreshUi()
    }
}
