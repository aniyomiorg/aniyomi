package eu.kanade.tachiyomi.data.download.anime.model

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import rx.subjects.PublishSubject
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.interactor.GetEpisode
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class AnimeDownload(
    val source: AnimeHttpSource,
    val anime: Anime,
    val episode: Episode,
    val changeDownloader: Boolean = false,
    var video: Video? = null,
) {

    @Volatile
    @Transient
    var totalProgress: Int = 0
        set(progress) {
            field = progress
            progressSubject?.onNext(this)
            progressCallback?.invoke(this)
        }

    @Volatile
    @Transient
    var downloadedImages: Int = 0

    @Transient
    private val _statusFlow = MutableStateFlow(State.NOT_DOWNLOADED)

    @Transient
    val statusFlow = _statusFlow.asStateFlow()
    var status: State
        get() = _statusFlow.value
        set(status) {
            _statusFlow.value = status
        }

    @Transient
    val progressFlow = flow {
        if (video == null) {
            emit(0)
            while (video == null) {
                delay(50)
            }
        }

        val progressFlows = video!!.progressFlow
        emitAll(combine(progressFlows) { it.average().toInt() })
    }
        .distinctUntilChanged()
        .debounce(50)

    @Transient
    var progressSubject: PublishSubject<AnimeDownload>? = null

    @Transient
    var progressCallback: ((AnimeDownload) -> Unit)? = null

    val progress: Int
        get() {
            val video = video ?: return 0
            return video.progress
        }

    enum class State(val value: Int) {
        NOT_DOWNLOADED(0),
        QUEUE(1),
        DOWNLOADING(2),
        DOWNLOADED(3),
        ERROR(4),
    }

    companion object {
        suspend fun fromEpisodeId(
            episodeId: Long,
            getEpisode: GetEpisode = Injekt.get(),
            getAnimeById: GetAnime = Injekt.get(),
            sourceManager: AnimeSourceManager = Injekt.get(),
        ): AnimeDownload? {
            val episode = getEpisode.await(episodeId) ?: return null
            val anime = getAnimeById.await(episode.animeId) ?: return null
            val source = sourceManager.get(anime.source) as? AnimeHttpSource ?: return null

            return AnimeDownload(source, anime, episode)
        }
    }
}
