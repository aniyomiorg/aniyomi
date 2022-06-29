package eu.kanade.tachiyomi.ui.anime.episode.base

import android.view.View
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.presentation.manga.EpisodeDownloadAction

open class BaseEpisodeHolder(
    view: View,
    private val adapter: BaseEpisodesAdapter<*>,
) : FlexibleViewHolder(view, adapter) {

    val downloadActionListener: (EpisodeDownloadAction) -> Unit = { action ->
        when (action) {
            EpisodeDownloadAction.START -> {
                adapter.clickListener.downloadEpisode(bindingAdapterPosition)
            }
            EpisodeDownloadAction.START_NOW -> {
                adapter.clickListener.startDownloadNow(bindingAdapterPosition)
            }
            EpisodeDownloadAction.CANCEL, EpisodeDownloadAction.DELETE -> {
                adapter.clickListener.deleteEpisode(bindingAdapterPosition)
            }
            EpisodeDownloadAction.START_ALT -> {
                adapter.clickListener.downloadEpisodeExternally(bindingAdapterPosition)
            }
        }
    }
}
