package eu.kanade.tachiyomi.ui.download.anime

import android.os.Bundle
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.download.model.AnimeDownloadQueue
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

/**
 * Presenter of [DownloadController].
 */
class DownloadPresenter : BasePresenter<DownloadController>() {

    val downloadManager: AnimeDownloadManager by injectLazy()

    /**
     * Property to get the queue from the download manager.
     */
    val downloadQueue: AnimeDownloadQueue
        get() = downloadManager.queue

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        downloadQueue.getUpdatedObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.map(::DownloadItem) }
            .subscribeLatestCache(DownloadController::onNextDownloads) { _, error ->
                Timber.e(error)
            }
    }

    fun getDownloadStatusObservable(): Observable<AnimeDownload> {
        return downloadQueue.getStatusObservable()
            .startWith(downloadQueue.getActiveDownloads())
    }

    fun getDownloadProgressObservable(): Observable<AnimeDownload> {
        return downloadQueue.getProgressObservable()
            .onBackpressureBuffer()
    }

    /**
     * Pauses the download queue.
     */
    fun pauseDownloads() {
        downloadManager.pauseDownloads()
    }

    /**
     * Clears the download queue.
     */
    fun clearQueue() {
        downloadManager.clearQueue()
    }

    fun reorder(downloads: List<AnimeDownload>) {
        downloadManager.reorderQueue(downloads)
    }

    fun cancelDownload(download: AnimeDownload) {
        downloadManager.deletePendingDownload(download)
    }

    fun cancelDownloads(downloads: List<AnimeDownload>) {
        downloadManager.deletePendingDownloads(*downloads.toTypedArray())
    }
}
