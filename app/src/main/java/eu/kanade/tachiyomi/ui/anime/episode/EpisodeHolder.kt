package eu.kanade.tachiyomi.ui.anime.episode

import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.databinding.EpisodesItemBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.anime.episode.base.BaseEpisodeHolder
import java.util.Date

class EpisodeHolder(
    view: View,
    private val adapter: EpisodesAdapter
) : BaseEpisodeHolder(view, adapter) {

    private val binding = EpisodesItemBinding.bind(view)

    init {
        binding.animedownload.setOnClickListener {
            onAnimeDownloadClick(it, bindingAdapterPosition)
        }
    }

    fun bind(item: EpisodeItem, anime: Anime) {
        val episode = item.episode

        binding.episodeTitle.text = when (anime.displayMode) {
            Anime.DISPLAY_NUMBER -> {
                val number = adapter.decimalFormat.format(episode.episode_number.toDouble())
                itemView.context.getString(R.string.display_mode_chapter, number)
            }
            else -> episode.name
        }

        // Set correct text color
        val episodeTitleColor = when {
            episode.seen -> adapter.readColor
            episode.bookmark -> adapter.bookmarkedColor
            else -> adapter.unreadColor
        }
        binding.episodeTitle.setTextColor(episodeTitleColor)

        val episodeDescriptionColor = when {
            episode.seen -> adapter.readColor
            episode.bookmark -> adapter.bookmarkedColor
            else -> adapter.unreadColorSecondary
        }
        binding.episodeDescription.setTextColor(episodeDescriptionColor)

        binding.bookmarkIcon.isVisible = episode.bookmark

        val descriptions = mutableListOf<CharSequence>()

        if (episode.date_upload > 0) {
            descriptions.add(adapter.dateFormat.format(Date(episode.date_upload)))
        }
        if (!episode.seen && episode.last_second_seen > 0) {
            val lastPageRead = buildSpannedString {
                color(adapter.readColor) {
                    append(itemView.context.getString(R.string.chapter_progress, episode.last_second_seen + 1))
                }
            }
            descriptions.add(lastPageRead)
        }
        if (!episode.scanlator.isNullOrBlank()) {
            descriptions.add(episode.scanlator!!)
        }

        if (descriptions.isNotEmpty()) {
            binding.episodeDescription.text = descriptions.joinTo(SpannableStringBuilder(), " • ")
        } else {
            binding.episodeDescription.text = ""
        }

        binding.animedownload.isVisible = item.anime.source != LocalSource.ID
        binding.animedownload.setState(item.status, item.progress)
    }
}
