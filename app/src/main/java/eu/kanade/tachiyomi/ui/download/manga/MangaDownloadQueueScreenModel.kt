package eu.kanade.tachiyomi.ui.download.manga

import android.content.Context
import android.view.MenuItem
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadService
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class MangaDownloadQueueScreenModel(
    private val downloadManager: MangaDownloadManager = Injekt.get(),
) : ScreenModel {

    private val _state = MutableStateFlow(emptyList<MangaDownloadHeaderItem>())
    val state = _state.asStateFlow()

    lateinit var controllerBinding: DownloadListBinding

    /**
     * Adapter containing the active downloads.
     */
    var adapter: MangaDownloadAdapter? = null

    /**
     * Map of subscriptions for active downloads.
     */
    val progressSubscriptions by lazy { mutableMapOf<MangaDownload, Subscription>() }

    val listener = object : MangaDownloadAdapter.DownloadItemListener {
        /**
         * Called when an item is released from a drag.
         *
         * @param position The position of the released item.
         */
        override fun onItemReleased(position: Int) {
            val adapter = adapter ?: return
            val downloads = adapter.headerItems.flatMap { header ->
                adapter.getSectionItems(header).map { item ->
                    (item as MangaDownloadItem).download
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
            if (item is MangaDownloadItem) {
                when (menuItem.itemId) {
                    R.id.move_to_top, R.id.move_to_bottom -> {
                        val headerItems = adapter?.headerItems ?: return
                        val newDownloads = mutableListOf<MangaDownload>()
                        headerItems.forEach { headerItem ->
                            headerItem as MangaDownloadHeaderItem
                            if (headerItem == item.header) {
                                headerItem.removeSubItem(item)
                                if (menuItem.itemId == R.id.move_to_top) {
                                    headerItem.addSubItem(0, item)
                                } else {
                                    headerItem.addSubItem(item)
                                }
                            }
                            newDownloads.addAll(headerItem.subItems.map { it.download })
                        }
                        reorder(newDownloads)
                    }
                    R.id.move_to_top_series -> {
                        val (selectedSeries, otherSeries) = adapter?.currentItems
                            ?.filterIsInstance<MangaDownloadItem>()
                            ?.map(MangaDownloadItem::download)
                            ?.partition { item.download.manga.id == it.manga.id }
                            ?: Pair(emptyList(), emptyList())
                        reorder(selectedSeries + otherSeries)
                    }
                    R.id.cancel_download -> {
                        cancel(listOf(item.download))
                    }
                    R.id.cancel_series -> {
                        val allDownloadsForSeries = adapter?.currentItems
                            ?.filterIsInstance<MangaDownloadItem>()
                            ?.filter { item.download.manga.id == it.download.manga.id }
                            ?.map(MangaDownloadItem::download)
                        if (!allDownloadsForSeries.isNullOrEmpty()) {
                            cancel(allDownloadsForSeries)
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
                            MangaDownloadHeaderItem(entry.key.id, entry.key.name, entry.value.size).apply {
                                addSubItems(0, entry.value.map { MangaDownloadItem(it, this) })
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
        MangaDownloadService.stop(context)
        downloadManager.clearQueue()
    }

    fun reorder(downloads: List<MangaDownload>) {
        downloadManager.reorderQueue(downloads)
    }

    fun cancel(downloads: List<MangaDownload>) {
        downloadManager.cancelQueuedDownloads(downloads)
    }

    fun <R : Comparable<R>> reorderQueue(selector: (MangaDownloadItem) -> R, reverse: Boolean = false) {
        val adapter = adapter ?: return
        val newDownloads = mutableListOf<MangaDownload>()
        adapter.headerItems.forEach { headerItem ->
            headerItem as MangaDownloadHeaderItem
            headerItem.subItems = headerItem.subItems.sortedBy(selector).toMutableList().apply {
                if (reverse) {
                    reverse()
                }
            }
            newDownloads.addAll(headerItem.subItems.map { it.download })
        }
        reorder(newDownloads)
    }

    /**
     * Called when the status of a download changes.
     *
     * @param download the download whose status has changed.
     */
    fun onStatusChange(download: MangaDownload) {
        when (download.status) {
            MangaDownload.State.DOWNLOADING -> {
                observeProgress(download)
                // Initial update of the downloaded pages
                onUpdateDownloadedPages(download)
            }
            MangaDownload.State.DOWNLOADED -> {
                unsubscribeProgress(download)
                onUpdateProgress(download)
                onUpdateDownloadedPages(download)
            }
            MangaDownload.State.ERROR -> unsubscribeProgress(download)
            else -> {
                /* unused */
            }
        }
    }

    /**
     * Observe the progress of a download and notify the view.
     *
     * @param download the download to observe its progress.
     */
    private fun observeProgress(download: MangaDownload) {
        val subscription = Observable.interval(50, TimeUnit.MILLISECONDS)
            // Get the sum of percentages for all the pages.
            .flatMap {
                Observable.from(download.pages)
                    .map(Page::progress)
                    .reduce { x, y -> x + y }
            }
            // Keep only the latest emission to avoid backpressure.
            .onBackpressureLatest()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { progress ->
                // Update the view only if the progress has changed.
                if (download.totalProgress != progress) {
                    download.totalProgress = progress
                    onUpdateProgress(download)
                }
            }

        // Avoid leaking subscriptions
        progressSubscriptions.remove(download)?.unsubscribe()

        progressSubscriptions[download] = subscription
    }

    /**
     * Unsubscribes the given download from the progress subscriptions.
     *
     * @param download the download to unsubscribe.
     */
    private fun unsubscribeProgress(download: MangaDownload) {
        progressSubscriptions.remove(download)?.unsubscribe()
    }

    /**
     * Called when the progress of a download changes.
     *
     * @param download the download whose progress has changed.
     */
    private fun onUpdateProgress(download: MangaDownload) {
        getHolder(download)?.notifyProgress()
    }

    /**
     * Called when a page of a download is downloaded.
     *
     * @param download the download whose page has been downloaded.
     */
    fun onUpdateDownloadedPages(download: MangaDownload) {
        getHolder(download)?.notifyDownloadedPages()
    }

    /**
     * Returns the holder for the given download.
     *
     * @param download the download to find.
     * @return the holder of the download or null if it's not bound.
     */
    private fun getHolder(download: MangaDownload): MangaDownloadHolder? {
        return controllerBinding.recycler.findViewHolderForItemId(download.chapter.id) as? MangaDownloadHolder
    }
}
