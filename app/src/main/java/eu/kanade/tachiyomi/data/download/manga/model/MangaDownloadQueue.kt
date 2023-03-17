package eu.kanade.tachiyomi.data.download.manga.model

import eu.kanade.core.util.asFlow
import eu.kanade.domain.entries.manga.model.Manga
import eu.kanade.domain.items.chapter.model.Chapter
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadStore
import eu.kanade.tachiyomi.source.model.Page
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

class MangaDownloadQueue(
    private val store: MangaDownloadStore,
    private val queue: MutableList<MangaDownload> = CopyOnWriteArrayList(),
) : List<MangaDownload> by queue {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val statusSubject = PublishSubject.create<MangaDownload>()

    private val _updates: Channel<Unit> = Channel(Channel.UNLIMITED)
    val updates = _updates.receiveAsFlow()
        .onStart { emit(Unit) }
        .map { queue }
        .shareIn(scope, SharingStarted.Eagerly, 1)

    fun addAll(downloads: List<MangaDownload>) {
        downloads.forEach { download ->
            download.statusSubject = statusSubject
            download.statusCallback = ::setPagesFor
            download.status = MangaDownload.State.QUEUE
        }
        queue.addAll(downloads)
        store.addAll(downloads)
        scope.launchNonCancellable {
            _updates.send(Unit)
        }
    }

    fun remove(download: MangaDownload) {
        val removed = queue.remove(download)
        store.remove(download)
        download.statusSubject = null
        download.statusCallback = null
        if (download.status == MangaDownload.State.DOWNLOADING || download.status == MangaDownload.State.QUEUE) {
            download.status = MangaDownload.State.NOT_DOWNLOADED
        }
        if (removed) {
            scope.launchNonCancellable {
                _updates.send(Unit)
            }
        }
    }

    fun remove(chapter: Chapter) {
        find { it.chapter.id == chapter.id }?.let { remove(it) }
    }

    fun remove(chapters: List<Chapter>) {
        chapters.forEach(::remove)
    }

    fun remove(manga: Manga) {
        filter { it.manga.id == manga.id }.forEach { remove(it) }
    }

    fun clear() {
        queue.forEach { download ->
            download.statusSubject = null
            download.statusCallback = null
            if (download.status == MangaDownload.State.DOWNLOADING || download.status == MangaDownload.State.QUEUE) {
                download.status = MangaDownload.State.NOT_DOWNLOADED
            }
        }
        queue.clear()
        store.clear()
        scope.launchNonCancellable {
            _updates.send(Unit)
        }
    }

    fun statusFlow(): Flow<MangaDownload> = getStatusObservable().asFlow()

    fun progressFlow(): Flow<MangaDownload> = getProgressObservable().asFlow()

    private fun getActiveDownloads(): Observable<MangaDownload> =
        Observable.from(this).filter { download -> download.status == MangaDownload.State.DOWNLOADING }

    private fun getStatusObservable(): Observable<MangaDownload> = statusSubject
        .startWith(getActiveDownloads())
        .onBackpressureBuffer()

    private fun getProgressObservable(): Observable<MangaDownload> {
        return statusSubject.onBackpressureBuffer()
            .startWith(getActiveDownloads())
            .flatMap { download ->
                if (download.status == MangaDownload.State.DOWNLOADING) {
                    val pageStatusSubject = PublishSubject.create<Page.State>()
                    setPagesSubject(download.pages, pageStatusSubject)
                    return@flatMap pageStatusSubject
                        .onBackpressureBuffer()
                        .filter { it == Page.State.READY }
                        .map { download }
                } else if (download.status == MangaDownload.State.DOWNLOADED || download.status == MangaDownload.State.ERROR) {
                    setPagesSubject(download.pages, null)
                }
                Observable.just(download)
            }
            .filter { it.status == MangaDownload.State.DOWNLOADING }
    }

    private fun setPagesFor(download: MangaDownload) {
        if (download.status == MangaDownload.State.DOWNLOADED || download.status == MangaDownload.State.ERROR) {
            setPagesSubject(download.pages, null)
        }
    }

    private fun setPagesSubject(pages: List<Page>?, subject: PublishSubject<Page.State>?) {
        pages?.forEach { it.statusSubject = subject }
    }
}
