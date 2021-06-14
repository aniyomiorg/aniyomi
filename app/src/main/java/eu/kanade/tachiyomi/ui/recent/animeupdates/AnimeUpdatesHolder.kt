package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.view.View
import androidx.core.view.isVisible
import coil.clear
import coil.loadAny
import coil.transform.RoundedCornersTransformation
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.databinding.AnimeUpdatesItemBinding
import eu.kanade.tachiyomi.ui.anime.episode.base.BaseEpisodeHolder

/**
 * Holder that contains episode item
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new recent episode holder.
 */
class AnimeUpdatesHolder(private val view: View, private val adapter: AnimeUpdatesAdapter) :
    BaseEpisodeHolder(view, adapter) {

    private val binding = AnimeUpdatesItemBinding.bind(view)

    init {
        binding.animeCover.setOnClickListener {
            adapter.coverClickListener.onCoverClick(bindingAdapterPosition)
        }

        binding.download.setOnClickListener {
            onAnimeDownloadClick(it, bindingAdapterPosition)
        }
    }

    fun bind(item: AnimeUpdatesItem) {
        // Set episode title
        binding.episodeTitle.text = item.episode.name

        // Set anime title
        binding.animeTitle.text = item.anime.title

        // Check if episode is read and set correct color
        if (item.episode.seen) {
            binding.episodeTitle.setTextColor(adapter.seenColor)
            binding.animeTitle.setTextColor(adapter.seenColor)
        } else {
            binding.episodeTitle.setTextColor(adapter.unseenColor)
            binding.animeTitle.setTextColor(adapter.unseenColor)
        }

        // Set episode status
        binding.download.isVisible = item.anime.source != LocalAnimeSource.ID
        binding.download.setState(item.status, item.progress)

        // Set cover
        val radius = itemView.context.resources.getDimension(R.dimen.card_radius)
        binding.animeCover.clear()
        binding.animeCover.loadAny(item.anime) {
            transformations(RoundedCornersTransformation(radius))
        }
    }
}
