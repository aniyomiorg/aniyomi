package eu.kanade.tachiyomi.ui.player

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.view.children
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.databinding.PlayerTracksItemBinding
import eu.kanade.tachiyomi.databinding.PlayerTracksSheetBinding
import eu.kanade.tachiyomi.widget.sheet.PlayerBottomSheetDialog

/**
 * Sheet to show when track selection buttons in player are clicked.
 */
class PlayerTracksSheet(
    private val activity: PlayerActivity,
    private val textRes: Int,
    private val changeTrackMethod: (Int) -> Unit,
    private val tracks: Array<Track>,
    private val preselectedTrack: Int,
) : PlayerBottomSheetDialog(activity) {

    private lateinit var binding: PlayerTracksSheetBinding
    private var wasPaused: Boolean? = null

    override fun createView(inflater: LayoutInflater): View {
        wasPaused = activity.player.paused
        activity.player.paused = true
        binding = PlayerTracksSheetBinding.inflate(activity.layoutInflater, null, false)

        binding.trackSelectionHeader.setText(textRes)
        tracks.forEachIndexed { i, track ->
            val trackView = PlayerTracksItemBinding.inflate(activity.layoutInflater).root
            trackView.text = track.lang
            if (preselectedTrack == i) {
                trackView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_24dp, 0)
            }
            trackView.setOnClickListener {
                clearSelection()
                (it as? TextView)?.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_24dp, 0)
                changeTrackMethod(i)
            }
            binding.linearLayout.addView(trackView)
        }

        return binding.root
    }

    private fun clearSelection() {
        binding.linearLayout.children.forEach {
            val view = it as? TextView ?: return
            if (view.id != R.id.track_selection_header) {
                view.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_blank_24dp, 0)
            }
        }
    }

    override fun dismiss() {
        activity.playerControls.showAndFadeControls()
        wasPaused?.let { activity.player.paused = it }
        super.dismiss()
        activity.setVisibilities()
    }
}
