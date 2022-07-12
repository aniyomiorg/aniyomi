package eu.kanade.tachiyomi.ui.player

import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PlayerOptionsSheetBinding
import eu.kanade.tachiyomi.widget.sheet.PlayerBottomSheetDialog

/**
 * Sheet to show when overflow button in player is clicked.
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

        binding.setAsCover.setOnClickListener { setAsCover() }
        binding.share.setOnClickListener { share() }
        binding.save.setOnClickListener { save() }
        binding.toggleSubs.isChecked = activity.screenshotSubs
        binding.toggleSubs.setOnCheckedChangeListener { _, newValue -> activity.screenshotSubs = newValue }
        binding.toggleVolumeBrightnessGestures.isChecked = activity.gestureVolumeBrightness
        binding.toggleVolumeBrightnessGestures.setOnCheckedChangeListener { _, newValue -> activity.gestureVolumeBrightness = newValue }
        binding.toggleHorizontalSeekGesture.isChecked = activity.gestureHorizontalSeek
        binding.toggleHorizontalSeekGesture.setOnCheckedChangeListener { _, newValue -> activity.gestureHorizontalSeek = newValue }
        binding.toggleStats.isChecked = activity.stats
        binding.statsPage.isVisible = activity.stats
        binding.toggleStats.setOnCheckedChangeListener(toggleStats)
        binding.statsPage.setSelection(activity.statsPage)
        binding.statsPage.onItemSelectedListener = setStatsPage

        return binding.root
    }

    /**
     * Sets the screenshot as the cover of the anime.
     */
    private fun setAsCover() {
        MaterialAlertDialogBuilder(activity)
            .setMessage(R.string.confirm_set_image_as_cover)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                activity.setAsCover()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /**
     * Shares the screenshot with external apps.
     */
    private fun share() {
        activity.shareImage()
    }

    /**
     * Saves the screenshot on external storage.
     */
    private fun save() {
        activity.saveImage()
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
        activity.setVisibilities()
    }
}
