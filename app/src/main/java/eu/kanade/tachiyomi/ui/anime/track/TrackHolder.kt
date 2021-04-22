package eu.kanade.tachiyomi.ui.anime.track

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.TrackItemBinding
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat

class TrackHolder(private val binding: TrackItemBinding, adapter: TrackAdapter) : RecyclerView.ViewHolder(binding.root) {

    private val preferences: PreferencesHelper by injectLazy()

    private val dateFormat: DateFormat by lazy {
        preferences.dateFormat()
    }

    init {
        val listener = adapter.rowClickListener

        binding.logoContainer.setOnClickListener { listener.onLogoClick(bindingAdapterPosition) }
        binding.trackSet.setOnClickListener { listener.onSetClick(bindingAdapterPosition) }
        binding.trackTitle.setOnClickListener { listener.onSetClick(bindingAdapterPosition) }
        binding.trackTitle.setOnLongClickListener {
            listener.onTitleLongClick(bindingAdapterPosition)
            true
        }
        binding.trackStatus.setOnClickListener { listener.onStatusClick(bindingAdapterPosition) }
        binding.trackChapters.setOnClickListener { listener.onEpisodesClick(bindingAdapterPosition) }
        binding.trackScore.setOnClickListener { listener.onScoreClick(bindingAdapterPosition) }
        binding.trackStartDate.setOnClickListener { listener.onStartDateClick(bindingAdapterPosition) }
        binding.trackFinishDate.setOnClickListener { listener.onFinishDateClick(bindingAdapterPosition) }
    }

    @SuppressLint("SetTextI18n")
    fun bind(item: TrackItem) {
        val track = item.track
        binding.trackLogo.setImageResource(item.service.getLogo())
        binding.logoContainer.setBackgroundColor(item.service.getLogoColor())

        binding.trackSet.isVisible = track == null
        binding.trackTitle.isVisible = track != null

        binding.trackDetails.isVisible = track != null
        if (track != null) {
            binding.trackTitle.text = track.title
            binding.trackChapters.text = "${track.last_episode_seen}/" +
                if (track.total_episodes > 0) track.total_episodes else "-"
            binding.trackStatus.text = item.service.getStatus(track.status)
            binding.trackScore.text = if (track.score == 0f) "-" else item.service.displayScore(track)

            if (item.service.supportsReadingDates) {
                binding.trackStartDate.text =
                    if (track.started_watching_date != 0L) dateFormat.format(track.started_watching_date) else "-"
                binding.trackFinishDate.text =
                    if (track.finished_watching_date != 0L) dateFormat.format(track.finished_watching_date) else "-"
            } else {
                binding.bottomDivider.isVisible = false
                binding.vertDivider3.isVisible = false
                binding.trackStartDate.isVisible = false
                binding.trackFinishDate.isVisible = false
            }
        }
    }
}
