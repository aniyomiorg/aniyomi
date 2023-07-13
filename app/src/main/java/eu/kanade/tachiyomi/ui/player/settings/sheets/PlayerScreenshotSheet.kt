package eu.kanade.tachiyomi.ui.player.settings.sheets

import android.view.LayoutInflater
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PlayerScreenshotSheetBinding
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.widget.sheet.PlayerBottomSheetDialog

/**
 * Sheet to show when the player is long clicked.
 */
class PlayerScreenshotSheet(
    private val activity: PlayerActivity,
) : PlayerBottomSheetDialog(activity) {

    private lateinit var binding: PlayerScreenshotSheetBinding

    override fun createView(inflater: LayoutInflater): View {
        binding = PlayerScreenshotSheetBinding.inflate(activity.layoutInflater, null, false)

        binding.setAsCover.setOnClickListener { setAsCover() }
        binding.share.setOnClickListener { share() }
        binding.save.setOnClickListener { save() }

        val screenshotSubtitles = activity.playerPreferences.screenshotSubtitles()

        binding.toggleSubs.isChecked = screenshotSubtitles.get()
        binding.toggleSubs.setOnCheckedChangeListener { _, newValue -> screenshotSubtitles.set(newValue) }

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
}
