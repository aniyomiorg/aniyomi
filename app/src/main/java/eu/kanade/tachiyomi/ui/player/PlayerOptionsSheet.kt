package eu.kanade.tachiyomi.ui.player

import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PlayerOptionsSheetBinding
import eu.kanade.tachiyomi.widget.sheet.BaseBottomSheetDialog

/**
 * Sheet to show when overflow button in player is clicked.
 */
class PlayerOptionsSheet(
    private val activity: PlayerActivity,
) : BaseBottomSheetDialog(activity) {

    private lateinit var binding: PlayerOptionsSheetBinding
    private var wasPaused: Boolean? = null

    override fun createView(inflater: LayoutInflater): View {
        wasPaused = activity.player.paused
        activity.player.paused = true
        binding = PlayerOptionsSheetBinding.inflate(activity.layoutInflater, null, false)

        binding.setAsCover.setOnClickListener { setAsCover() }
        binding.share.setOnClickListener { share() }
        binding.save.setOnClickListener { save() }
        binding.toggleStats.isChecked = activity.stats
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
        dismiss()
    }

    /**
     * Saves the screenshot on external storage.
     */
    private fun save() {
        activity.saveImage()
        dismiss()
    }

    private val toggleStats = { _: CompoundButton, _: Boolean ->
        activity.toggleStats()
        dismiss()
    }

    private val setStatsPage = { page: Int ->
        activity.statsPage = page
        dismiss()
    }

    override fun dismiss() {
        wasPaused?.let { activity.player.paused = it }
        super.dismiss()
    }
}
