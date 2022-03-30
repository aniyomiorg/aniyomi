package eu.kanade.tachiyomi.ui.recent.animehistory

import android.view.View
import coil.dispose
import coil.load
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.AnimeEpisodeHistory
import eu.kanade.tachiyomi.databinding.HistoryItemBinding
import eu.kanade.tachiyomi.util.lang.toTimestampString
import java.util.Date

/**
 * Holder that contains recent anime item
 * Uses R.layout.item_recently_read.
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new recent chapter holder.
 */
class AnimeHistoryHolder(
    view: View,
    val adapter: AnimeHistoryAdapter
) : FlexibleViewHolder(view, adapter) {

    private val binding = HistoryItemBinding.bind(view)

    init {
        binding.holder.setOnClickListener {
            adapter.itemClickListener.onItemClick(bindingAdapterPosition)
        }

        binding.remove.setOnClickListener {
            adapter.removeClickListener.onRemoveClick(bindingAdapterPosition)
        }

        binding.resume.setOnClickListener {
            adapter.resumeClickListener.onResumeClick(bindingAdapterPosition)
        }
    }

    /**
     * Set values of view
     *
     * @param item item containing animehistory information
     */
    fun bind(item: AnimeEpisodeHistory) {
        // Retrieve objects
        val (anime, chapter, animehistory) = item

        // Set anime title
        binding.mangaTitle.text = anime.title

        // Set chapter number + timestamp
        if (chapter.episode_number > -1f) {
            val formattedNumber = adapter.decimalFormat.format(chapter.episode_number.toDouble())
            binding.mangaSubtitle.text = itemView.context.getString(
                R.string.recent_anime_time,
                formattedNumber,
                Date(animehistory.last_seen).toTimestampString()
            )
        } else {
            binding.mangaSubtitle.text = Date(animehistory.last_seen).toTimestampString()
        }

        // Set cover
        binding.cover.dispose()
        binding.cover.load(item.anime)
    }
}
