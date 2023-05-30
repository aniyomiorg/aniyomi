package eu.kanade.tachiyomi.data.download.anime.model

import eu.kanade.core.util.asFlow
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadStore
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
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import java.util.concurrent.CopyOnWriteArrayList

class AnimeDownloadQueue(
    private val store: AnimeDownloadStore,
) {
    private val _state = MutableStateFlow<List<AnimeDownload>>(emptyList())
    val state = _state.asStateFlow()

    private val progressSubject = PublishSubject.create<AnimeDownload>()

    fun addAll(downloads: List<AnimeDownload>) {
        _state.update {
            downloads.forEach { download ->
                download.progressSubject = progressSubject
                download.progressCallback = ::setProgressFor
                download.status = AnimeDownload.State.QUEUE
            }
            store.addAll(downloads)
            it + downloads
        }
    }

    fun remove(download: AnimeDownload) {
        _state.update {
            store.remove(download)
            download.progressSubject = null
            download.progressCallback = null
            if (download.status == AnimeDownload.State.DOWNLOADING || download.status == AnimeDownload.State.QUEUE) {
                download.status = AnimeDownload.State.NOT_DOWNLOADED
            }
            it - download
        }
    }

    fun remove(episode: Episode) {
        _state.value.find { it.episode.id == episode.id }?.let { remove(it) }
    }

    fun remove(episodes: List<Episode>) {
        episodes.forEach(::remove)
    }

    fun remove(anime: Anime) {
        _state.value.filter { it.anime.id == anime.id }.forEach { remove(it) }
    }

    fun clear() {
        _state.update {
            it.forEach { download ->
                download.progressSubject = null
                download.progressCallback = null
                if (download.status == AnimeDownload.State.DOWNLOADING || download.status == AnimeDownload.State.QUEUE) {
                    download.status = AnimeDownload.State.NOT_DOWNLOADED
                }
            }
            store.clear()
            emptyList()
        }
    }

    fun statusFlow(): Flow<AnimeDownload> = state
        .flatMapLatest { downloads ->
            downloads
                .map { download ->
                    download.statusFlow.drop(1).map { download }
                }
                .merge()
        }
        .onStart { emitAll(getActiveDownloads()) }

    fun progressFlow(): Flow<AnimeDownload> = state
        .flatMapLatest { downloads ->
            downloads
                .map { download ->
                    download.progressFlow.drop(1).map { download }
                }
                .merge()
        }
        .onStart { emitAll(getActiveDownloads()) }

    private fun getActiveDownloads(): Flow<AnimeDownload> =
        _state.value.filter { download -> download.status == AnimeDownload.State.DOWNLOADING }.asFlow()

    fun count(predicate: (AnimeDownload) -> Boolean) = _state.value.count(predicate)
    fun filter(predicate: (AnimeDownload) -> Boolean) = _state.value.filter(predicate)
    fun find(predicate: (AnimeDownload) -> Boolean) = _state.value.find(predicate)
    fun <K> groupBy(keySelector: (AnimeDownload) -> K) = _state.value.groupBy(keySelector)
    fun isEmpty() = _state.value.isEmpty()
    fun isNotEmpty() = _state.value.isNotEmpty()
    fun none(predicate: (AnimeDownload) -> Boolean) = _state.value.none(predicate)
    fun toMutableList() = _state.value.toMutableList()

    private fun setProgressFor(download: AnimeDownload) {
        if (download.status == AnimeDownload.State.DOWNLOADED || download.status == AnimeDownload.State.ERROR) {
            setProgressSubject(download.video, null)
        }
    }

    private fun setProgressSubject(video: Video?, subject: PublishSubject<Video.State>?) {
        video?.progressSubject = subject
    }
}
