package eu.kanade.tachiyomi.ui.player.settings

import android.annotation.SuppressLint
import android.widget.TextView
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.databinding.PlayerTracksItemBinding
import eu.kanade.tachiyomi.databinding.PlayerTracksSheetBinding
import eu.kanade.tachiyomi.ui.player.PlayerActivity

/**
 * Sheet to show when track selection buttons in player are clicked.
 *
 * @param activity the instance of the PlayerActivity in use.
 * @param changeTrackMethod the method to run on changing tracks
 * @param tracks the given array of tracks
 * @param preselectedTrack the index of the current selected track
 * @param dismissSheet the method to run on selecting a track to dismiss the sheet
 * @param trackSettings the method to run on clicking the settings button, null if no button
 */
@SuppressLint("ViewConstructor")
class PlayerTracksSheet(
    private val activity: PlayerActivity,
    private val changeTrackMethod: (Int) -> Unit,
    private val tracks: Array<Track>,
    private val preselectedTrack: Int,
    private val dismissSheet: () -> Unit,
    private val trackSettings: (() -> Unit)?,
) : NestedScrollView(activity, null) {

    private val binding = PlayerTracksSheetBinding.inflate(activity.layoutInflater, null, false)

    init {
        addView(binding.root)
        initTracks()
    }

    private fun initTracks() {
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
                dismissSheet()
            }
            binding.linearLayout.addView(trackView)
        }
    }

    private fun clearSelection() {
        binding.linearLayout.children.forEach {
            val view = it as? TextView ?: return
            view.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_blank_24dp, 0)
        }
    }
}
