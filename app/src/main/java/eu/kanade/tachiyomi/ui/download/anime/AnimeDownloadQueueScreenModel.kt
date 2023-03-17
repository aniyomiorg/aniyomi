package eu.kanade.tachiyomi.ui.download.anime

import android.content.Context
import android.view.MenuItem
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadService
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import rx.Subscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeDownloadQueueScreenModel(
    private val downloadManager: AnimeDownloadManager = Injekt.get(),
) : ScreenModel {

    private val _state = MutableStateFlow(emptyList<AnimeDownloadHeaderItem>())
    val state = _state.asStateFlow()

    lateinit var controllerBinding: DownloadListBinding

    /**
     * Adapter containing the active downloads.
     */
    var adapter: AnimeDownloadAdapter? = null

    /**
     * Map of subscriptions for active downloads.
     */
    val progressSubscriptions by lazy { mutableMapOf<AnimeDownload, Subscription>() }

    val listener = object : AnimeDownloadAdapter.DownloadItemListener {
        /**
         * Called when an item is released from a drag.
         *
         * @param position The position of the released item.
         */
        override fun onItemReleased(position: Int) {
            val adapter = adapter ?: return
            val downloads = adapter.headerItems.flatMap { header ->
                adapter.getSectionItems(header).map { item ->
                    (item as AnimeDownloadItem).download
                }
            }
            reorder(downloads)
        }

        /**
         * Called when the menu item of a download is pressed
         *
         * @param position The position of the item
         * @param menuItem The menu Item pressed
         */
        override fun onMenuItemClick(position: Int, menuItem: MenuItem) {
            val item = adapter?.getItem(position) ?: return
            if (item is AnimeDownloadItem) {
                when (menuItem.itemId) {
                    R.id.move_to_top, R.id.move_to_bottom -> {
                        val headerItems = adapter?.headerItems ?: return
                        val newAnimeDownloads = mutableListOf<AnimeDownload>()
                        headerItems.forEach { headerItem ->
                            headerItem as AnimeDownloadHeaderItem
                            if (headerItem == item.header) {
                                headerItem.removeSubItem(item)
                                if (menuItem.itemId == R.id.move_to_top) {
                                    headerItem.addSubItem(0, item)
                                } else {
                                    headerItem.addSubItem(item)
                                }
                            }
                            newAnimeDownloads.addAll(headerItem.subItems.map { it.download })
                        }
                        reorder(newAnimeDownloads)
                    }
                    R.id.move_to_top_series -> {
                        val (selectedSeries, otherSeries) = adapter?.currentItems
                            ?.filterIsInstance<AnimeDownloadItem>()
                            ?.map(AnimeDownloadItem::download)
                            ?.partition { item.download.anime.id == it.anime.id }
                            ?: Pair(emptyList(), emptyList())
                        reorder(selectedSeries + otherSeries)
                    }
                    R.id.cancel_download -> {
                        cancel(listOf(item.download))
                    }
                    R.id.cancel_series -> {
                        val allAnimeDownloadsForSeries = adapter?.currentItems
                            ?.filterIsInstance<AnimeDownloadItem>()
                            ?.filter { item.download.anime.id == it.download.anime.id }
                            ?.map(AnimeDownloadItem::download)
                        if (!allAnimeDownloadsForSeries.isNullOrEmpty()) {
                            cancel(allAnimeDownloadsForSeries)
                        }
                    }
                }
            }
        }
    }

    init {
        coroutineScope.launch {
            downloadManager.queue.updates
                .catch { logcat(LogPriority.ERROR, it) }
                .map { downloads ->
                    downloads
                        .groupBy { it.source }
                        .map { entry ->
                            AnimeDownloadHeaderItem(entry.key.id, entry.key.name, entry.value.size).apply {
                                addSubItems(0, entry.value.map { AnimeDownloadItem(it, this) })
                            }
                        }
                }
                .collect { newList -> _state.update { newList } }
        }
    }

    override fun onDispose() {
        for (subscription in progressSubscriptions.values) {
            subscription.unsubscribe()
        }
        progressSubscriptions.clear()
        adapter = null
    }

    fun getDownloadStatusFlow() = downloadManager.queue.statusFlow()
    fun getDownloadProgressFlow() = downloadManager.queue.progressFlow()

    fun pauseDownloads() {
        downloadManager.pauseDownloads()
    }

    fun clearQueue(context: Context) {
        AnimeDownloadService.stop(context)
        downloadManager.clearQueue()
    }

    fun reorder(downloads: List<AnimeDownload>) {
        downloadManager.reorderQueue(downloads)
    }

    fun cancel(downloads: List<AnimeDownload>) {
        downloadManager.cancelQueuedDownloads(downloads)
    }

    fun <R : Comparable<R>> reorderQueue(selector: (AnimeDownloadItem) -> R, reverse: Boolean = false) {
        val adapter = adapter ?: return
        val newAnimeDownloads = mutableListOf<AnimeDownload>()
        adapter.headerItems.forEach { headerItem ->
            headerItem as AnimeDownloadHeaderItem
            headerItem.subItems = headerItem.subItems.sortedBy(selector).toMutableList().apply {
                if (reverse) {
                    reverse()
                }
            }
            newAnimeDownloads.addAll(headerItem.subItems.map { it.download })
        }
        reorder(newAnimeDownloads)
    }

    /**
     * Called when the status of a download changes.
     *
     * @param download the download whose status has changed.
     */
    fun onStatusChange(download: AnimeDownload) {
        when (download.status) {
            AnimeDownload.State.DOWNLOADING -> {
                // Initial update of the downloaded pages
                onUpdateProgress(download)
                onUpdateDownloadedPages(download)
            }
            AnimeDownload.State.DOWNLOADED -> {
                unsubscribeProgress(download)
                onUpdateProgress(download)
                onUpdateDownloadedPages(download)
            }
            AnimeDownload.State.ERROR -> unsubscribeProgress(download)
            else -> {
                /* unused */
            }
        }
    }

    /**
     * Unsubscribes the given download from the progress subscriptions.
     *
     * @param download the download to unsubscribe.
     */
    private fun unsubscribeProgress(download: AnimeDownload) {
        progressSubscriptions.remove(download)?.unsubscribe()
    }

    /**
     * Called when the progress of a download changes.
     *
     * @param download the download whose progress has changed.
     */
    private fun onUpdateProgress(download: AnimeDownload) {
        getHolder(download)?.notifyProgress()
        getHolder(download)?.notifyDownloadedPages()
    }

    /**
     * Called when a page of a download is downloaded.
     *
     * @param download the download whose page has been downloaded.
     */
    fun onUpdateDownloadedPages(download: AnimeDownload) {
        getHolder(download)?.notifyDownloadedPages()
        getHolder(download)?.notifyProgress()
    }

    /**
     * Returns the holder for the given download.
     *
     * @param download the download to find.
     * @return the holder of the download or null if it's not bound.
     */
    private fun getHolder(download: AnimeDownload): AnimeDownloadHolder? {
        return controllerBinding.recycler.findViewHolderForItemId(download.episode.id) as? AnimeDownloadHolder
    }
}
