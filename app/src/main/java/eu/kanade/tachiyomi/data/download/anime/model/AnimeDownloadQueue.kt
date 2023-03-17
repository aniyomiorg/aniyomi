package eu.kanade.tachiyomi.data.download.anime.model

import eu.kanade.core.util.asFlow
import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.domain.items.episode.model.Episode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadStore
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import rx.Observable
import rx.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList

class AnimeDownloadQueue(
    private val store: AnimeDownloadStore,
    val queue: MutableList<AnimeDownload> = CopyOnWriteArrayList(),
) : List<AnimeDownload> by queue {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val statusSubject = PublishSubject.create<AnimeDownload>()

    private val progressSubject = PublishSubject.create<AnimeDownload>()

    private val _updates: Channel<Unit> = Channel(Channel.UNLIMITED)
    val updates = _updates.receiveAsFlow()
        .onStart { emit(Unit) }
        .map { queue }
        .shareIn(scope, SharingStarted.Eagerly, 1)

    fun addAll(downloads: List<AnimeDownload>) {
        downloads.forEach { download ->
            download.statusSubject = statusSubject
            download.progressSubject = progressSubject
            download.statusCallback = ::setVideoFor
            download.progressCallback = ::setProgressFor
            download.status = AnimeDownload.State.QUEUE
        }
        queue.addAll(downloads)
        store.addAll(downloads)
        scope.launchNonCancellable {
            _updates.send(Unit)
        }
    }

    fun remove(download: AnimeDownload) {
        val removed = queue.remove(download)
        store.remove(download)
        download.statusSubject = null
        download.progressSubject = null
        download.statusCallback = null
        download.progressCallback = null
        if (download.status == AnimeDownload.State.DOWNLOADING || download.status == AnimeDownload.State.QUEUE) {
            download.status = AnimeDownload.State.NOT_DOWNLOADED
        }
        if (removed) {
            scope.launchNonCancellable {
                _updates.send(Unit)
            }
        }
    }

    fun remove(episode: Episode) {
        find { it.episode.id == episode.id }?.let { remove(it) }
    }

    fun remove(episodes: List<Episode>) {
        episodes.forEach(::remove)
    }

    fun remove(anime: Anime) {
        filter { it.anime.id == anime.id }.forEach { remove(it) }
    }

    fun clear() {
        queue.forEach { download ->
            download.statusSubject = null
            download.progressSubject = null
            download.statusCallback = null
            download.progressCallback = null
            if (download.status == AnimeDownload.State.DOWNLOADING || download.status == AnimeDownload.State.QUEUE) {
                download.status = AnimeDownload.State.NOT_DOWNLOADED
            }
        }
        queue.clear()
        store.clear()
        scope.launchNonCancellable {
            _updates.send(Unit)
        }
    }

    fun statusFlow(): Flow<AnimeDownload> = getStatusObservable().asFlow()

    fun progressFlow(): Flow<AnimeDownload> = getProgressObservable().asFlow()

    private fun getActiveDownloads(): Observable<AnimeDownload> =
        Observable.from(this).filter { download -> download.status == AnimeDownload.State.DOWNLOADING }

    private fun getStatusObservable(): Observable<AnimeDownload> = statusSubject
        .startWith(getActiveDownloads())
        .onBackpressureBuffer()

    private fun getProgressObservable(): Observable<AnimeDownload> {
        return progressSubject.onBackpressureLatest()
    }

    private fun setVideoFor(download: AnimeDownload) {
        if (download.status == AnimeDownload.State.DOWNLOADED || download.status == AnimeDownload.State.ERROR) {
            setVideoSubject(download.video, null)
        }
    }

    private fun setVideoSubject(video: Video?, subject: PublishSubject<Video.State>?) {
        video?.statusSubject = subject
    }

    private fun setProgressFor(download: AnimeDownload) {
        if (download.status == AnimeDownload.State.DOWNLOADED || download.status == AnimeDownload.State.ERROR) {
            setProgressSubject(download.video, null)
        }
    }

    private fun setProgressSubject(video: Video?, subject: PublishSubject<Video.State>?) {
        video?.progressSubject = subject
    }
}
