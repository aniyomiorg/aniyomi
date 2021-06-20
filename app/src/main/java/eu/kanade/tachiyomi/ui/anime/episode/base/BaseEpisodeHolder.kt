package eu.kanade.tachiyomi.ui.anime.episode.base

import android.view.View
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.util.view.popupMenu

open class BaseEpisodeHolder(
    view: View,
    private val adapter: BaseEpisodesAdapter<*>
) : FlexibleViewHolder(view, adapter) {

    fun onAnimeDownloadClick(view: View, position: Int) {
        val item = adapter.getItem(position) as? BaseEpisodeItem<*, *> ?: return
        when (item.status) {
            AnimeDownload.State.NOT_DOWNLOADED, AnimeDownload.State.ERROR -> {
                adapter.clickListener.downloadEpisode(position)
            }
            else -> {
                view.popupMenu(
                    R.menu.chapter_download,
                    initMenu = {
                        // AnimeDownload.State.DOWNLOADED
                        findItem(R.id.delete_download).isVisible = item.status == AnimeDownload.State.DOWNLOADED

                        // Download.State.QUEUE
                        findItem(R.id.start_download).isVisible = item.status == AnimeDownload.State.QUEUE
                        // AnimeDownload.State.DOWNLOADING, AnimeDownload.State.QUEUE
                        findItem(R.id.cancel_download).isVisible = item.status != AnimeDownload.State.DOWNLOADED
                    },
                    onMenuItemClick = {
                        if (itemId == R.id.start_download) {
                            adapter.clickListener.startDownloadNow(position)
                        } else {
                            adapter.clickListener.deleteEpisode(position)
                        }
                    }
                )
            }
        }
    }
}
