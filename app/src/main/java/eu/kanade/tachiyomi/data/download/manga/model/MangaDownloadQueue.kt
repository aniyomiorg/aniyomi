package eu.kanade.tachiyomi.data.download.manga.model

import eu.kanade.core.util.asFlow
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadStore
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import rx.Observable
import rx.subjects.PublishSubject
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.items.chapter.model.Chapter
import java.util.concurrent.CopyOnWriteArrayList

class MangaDownloadQueue(
    private val store: MangaDownloadStore,
) {
    private val _state = MutableStateFlow<List<MangaDownload>>(emptyList())
    val state = _state.asStateFlow()

    fun addAll(downloads: List<MangaDownload>) {
        _state.update {
            downloads.forEach { download ->
                download.status = MangaDownload.State.QUEUE
            }
            store.addAll(downloads)
            it + downloads
        }
    }

    fun remove(download: MangaDownload) {
        _state.update {
            store.remove(download)
            if (download.status == MangaDownload.State.DOWNLOADING || download.status == MangaDownload.State.QUEUE) {
                download.status = MangaDownload.State.NOT_DOWNLOADED
            }
            it - download
        }
    }

    fun remove(chapter: Chapter) {
        _state.value.find { it.chapter.id == chapter.id }?.let { remove(it) }
    }

    fun remove(chapters: List<Chapter>) {
        chapters.forEach(::remove)
    }

    fun remove(manga: Manga) {
        _state.value.filter { it.manga.id == manga.id }.forEach { remove(it) }
    }

    fun clear() {
        _state.update {
            it.forEach { download ->
                if (download.status == MangaDownload.State.DOWNLOADING || download.status == MangaDownload.State.QUEUE) {
                    download.status = MangaDownload.State.NOT_DOWNLOADED
                }
            }
            store.clear()
            emptyList()
        }
    }

    fun statusFlow(): Flow<MangaDownload> = state
        .flatMapLatest { downloads ->
            downloads
                .map { download ->
                    download.statusFlow.drop(1).map { download }
                }
                .merge()
        }
        .onStart { emitAll(getActiveDownloads()) }

    fun progressFlow(): Flow<MangaDownload> = state
        .flatMapLatest { downloads ->
            downloads
                .map { download ->
                    download.progressFlow.drop(1).map { download }
                }
                .merge()
        }
        .onStart { emitAll(getActiveDownloads()) }

    private fun getActiveDownloads(): Flow<MangaDownload> =
        _state.value.filter { download -> download.status == MangaDownload.State.DOWNLOADING }.asFlow()

    fun count(predicate: (MangaDownload) -> Boolean) = _state.value.count(predicate)
    fun filter(predicate: (MangaDownload) -> Boolean) = _state.value.filter(predicate)
    fun find(predicate: (MangaDownload) -> Boolean) = _state.value.find(predicate)
    fun <K> groupBy(keySelector: (MangaDownload) -> K) = _state.value.groupBy(keySelector)
    fun isEmpty() = _state.value.isEmpty()
    fun isNotEmpty() = _state.value.isNotEmpty()
    fun none(predicate: (MangaDownload) -> Boolean) = _state.value.none(predicate)
    fun toMutableList() = _state.value.toMutableList()
}
