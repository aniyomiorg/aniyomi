package eu.kanade.tachiyomi.ui.anime.track

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import coil.clear
import coil.load
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.databinding.AnimeTrackSearchItemBinding
import eu.kanade.tachiyomi.util.view.inflate

class TrackSearchAdapter(context: Context) :
    ArrayAdapter<AnimeTrackSearch>(context, R.layout.track_search_item, mutableListOf<AnimeTrackSearch>()) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var v = view
        // Get the data item for this position
        val track = getItem(position)!!
        // Check if an existing view is being reused, otherwise inflate the view
        val holder: TrackSearchHolder // view lookup cache stored in tag
        if (v == null) {
            v = parent.inflate(R.layout.track_search_item)
            holder = TrackSearchHolder(v)
            v.tag = holder
        } else {
            holder = v.tag as TrackSearchHolder
        }
        holder.onSetValues(track)
        return v
    }

    fun setItems(syncs: List<AnimeTrackSearch>) {
        setNotifyOnChange(false)
        clear()
        addAll(syncs)
        notifyDataSetChanged()
    }

    class TrackSearchHolder(private val view: View) {

        private val binding = AnimeTrackSearchItemBinding.bind(view)

        fun onSetValues(track: AnimeTrackSearch) {
            binding.trackSearchTitle.text = track.title
            binding.trackSearchSummary.text = track.summary
            binding.trackSearchCover.clear()
            if (track.cover_url.isNotEmpty()) {
                binding.trackSearchCover.load(track.cover_url)
            }

            val hasStatus = track.publishing_status.isNotBlank()
            binding.trackSearchStatus.isVisible = hasStatus
            binding.trackSearchStatusResult.isVisible = hasStatus
            if (hasStatus) {
                binding.trackSearchStatusResult.text = track.publishing_status.capitalize()
            }

            val hasType = track.publishing_type.isNotBlank()
            binding.trackSearchType.isVisible = hasType
            binding.trackSearchTypeResult.isVisible = hasType
            if (hasType) {
                binding.trackSearchTypeResult.text = track.publishing_type.capitalize()
            }

            val hasStartDate = track.start_date.isNotBlank()
            binding.trackSearchStart.isVisible = hasStartDate
            binding.trackSearchStartResult.isVisible = hasStartDate
            if (hasStartDate) {
                binding.trackSearchStartResult.text = track.start_date
            }
        }
    }
}
