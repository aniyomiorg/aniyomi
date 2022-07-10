package eu.kanade.tachiyomi.data.download.model

import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.episode.model.Episode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.data.download.AnimeDownloadStore
import rx.Observable
import rx.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList

class AnimeDownloadQueue(
    private val store: AnimeDownloadStore,
    val queue: MutableList<AnimeDownload> = CopyOnWriteArrayList(),
) : List<AnimeDownload> by queue {

    private val statusSubject = PublishSubject.create<AnimeDownload>()

    private val progressSubject = PublishSubject.create<AnimeDownload>()

    private val updatedRelay = PublishRelay.create<Unit>()

    fun addAll(downloads: List<AnimeDownload>) {
        downloads.forEach { download ->
            download.setStatusSubject(statusSubject)
            download.setProgressSubject(progressSubject)
            download.setStatusCallback(::setPagesFor)
            download.setProgressCallback(::setProgressFor)
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
        download.setProgressSubject(null)
        download.setStatusCallback(null)
        download.setProgressCallback(null)
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
            download.setProgressSubject(null)
            download.setStatusCallback(null)
            download.setProgressCallback(null)
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
            setPagesSubject(download.video, null)
        }
    }

    fun getProgressObservable(): Observable<AnimeDownload> {
        return statusSubject.onBackpressureBuffer()
            .startWith(getActiveDownloads())
            .flatMap { download ->
                if (download.status == AnimeDownload.State.DOWNLOADING) {
                    val pageStatusSubject = PublishSubject.create<Int>()
                    setPagesSubject(download.video, pageStatusSubject)
                    return@flatMap pageStatusSubject
                        .onBackpressureBuffer()
                        .filter { it == Video.READY }
                        .map { download }
                } else if (download.status == AnimeDownload.State.DOWNLOADED || download.status == AnimeDownload.State.ERROR) {
                    setPagesSubject(download.video, null)
                }
                Observable.just(download)
            }
            .filter { it.status == AnimeDownload.State.DOWNLOADING }
    }

    fun getPreciseProgressObservable(): Observable<AnimeDownload> {
        return progressSubject.onBackpressureLatest()
    }

    private fun setPagesSubject(video: Video?, subject: PublishSubject<Int>?) {
        video?.setStatusSubject(subject)
    }

    private fun setProgressFor(download: AnimeDownload) {
        if (download.status == AnimeDownload.State.DOWNLOADED || download.status == AnimeDownload.State.ERROR) {
            setProgressSubject(download.video, null)
        }
    }

    private fun setProgressSubject(video: Video?, subject: PublishSubject<Int>?) {
        video?.setProgressSubject(subject)
    }
}
