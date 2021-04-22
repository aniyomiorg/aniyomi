package eu.kanade.tachiyomi.data.download.model

import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.AnimeDownloadStore
import eu.kanade.tachiyomi.source.model.Page
import rx.Observable
import rx.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList

class AnimeDownloadQueue(
    private val store: AnimeDownloadStore,
    private val queue: MutableList<AnimeDownload> = CopyOnWriteArrayList()
) : List<AnimeDownload> by queue {

    private val statusSubject = PublishSubject.create<AnimeDownload>()

    private val updatedRelay = PublishRelay.create<Unit>()

    fun addAll(downloads: List<AnimeDownload>) {
        downloads.forEach { download ->
            download.setStatusSubject(statusSubject)
            download.setStatusCallback(::setPagesFor)
            download.status = AnimeDownload.State.QUEUE
        }
        queue.addAll(downloads)
        store.addAll(downloads)
        updatedRelay.call(Unit)
    }

    fun remove(download: AnimeDownload) {
        val removed = queue.remove(download)
        store.remove(download)
        download.setStatusSubject(null)
        download.setStatusCallback(null)
        if (download.status == AnimeDownload.State.DOWNLOADING || download.status == AnimeDownload.State.QUEUE) {
            download.status = AnimeDownload.State.NOT_DOWNLOADED
        }
        if (removed) {
            updatedRelay.call(Unit)
        }
    }

    fun remove(episode: Episode) {
        find { it.episode.id == episode.id }?.let { remove(it) }
    }

    fun remove(episodes: List<Episode>) {
        for (episode in episodes) {
            remove(episode)
        }
    }

    fun remove(anime: Anime) {
        filter { it.anime.id == anime.id }.forEach { remove(it) }
    }

    fun clear() {
        queue.forEach { download ->
            download.setStatusSubject(null)
            download.setStatusCallback(null)
            if (download.status == AnimeDownload.State.DOWNLOADING || download.status == AnimeDownload.State.QUEUE) {
                download.status = AnimeDownload.State.NOT_DOWNLOADED
            }
        }
        queue.clear()
        store.clear()
        updatedRelay.call(Unit)
    }

    fun getActiveDownloads(): Observable<AnimeDownload> =
        Observable.from(this).filter { download -> download.status == AnimeDownload.State.DOWNLOADING }

    fun getStatusObservable(): Observable<AnimeDownload> = statusSubject.onBackpressureBuffer()

    fun getUpdatedObservable(): Observable<List<AnimeDownload>> = updatedRelay.onBackpressureBuffer()
        .startWith(Unit)
        .map { this }

    private fun setPagesFor(download: AnimeDownload) {
        if (download.status == AnimeDownload.State.DOWNLOADED || download.status == AnimeDownload.State.ERROR) {
            setPagesSubject(download.pages, null)
        }
    }

    fun getProgressObservable(): Observable<AnimeDownload> {
        return statusSubject.onBackpressureBuffer()
            .startWith(getActiveDownloads())
            .flatMap { download ->
                if (download.status == AnimeDownload.State.DOWNLOADING) {
                    val pageStatusSubject = PublishSubject.create<Int>()
                    setPagesSubject(download.pages, pageStatusSubject)
                    return@flatMap pageStatusSubject
                        .onBackpressureBuffer()
                        .filter { it == Page.READY }
                        .map { download }
                } else if (download.status == AnimeDownload.State.DOWNLOADED || download.status == AnimeDownload.State.ERROR) {
                    setPagesSubject(download.pages, null)
                }
                Observable.just(download)
            }
            .filter { it.status == AnimeDownload.State.DOWNLOADING }
    }

    private fun setPagesSubject(pages: List<Page>?, subject: PublishSubject<Int>?) {
        pages?.forEach { it.setStatusSubject(subject) }
    }
}
