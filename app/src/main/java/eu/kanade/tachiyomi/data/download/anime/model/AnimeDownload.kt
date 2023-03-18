package eu.kanade.tachiyomi.data.download.anime.model

import eu.kanade.domain.entries.anime.interactor.GetAnime
import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.domain.items.episode.interactor.GetEpisode
import eu.kanade.domain.items.episode.model.Episode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import rx.subjects.PublishSubject
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

    @Volatile
    @Transient
    var status: State = State.NOT_DOWNLOADED
        set(status) {
            field = status
            statusSubject?.onNext(this)
            statusCallback?.invoke(this)
        }

    @Transient
    var statusSubject: PublishSubject<AnimeDownload>? = null

    @Transient
    var progressSubject: PublishSubject<AnimeDownload>? = null

    @Transient
    var statusCallback: ((AnimeDownload) -> Unit)? = null

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
            chapterId: Long,
            getEpisode: GetEpisode = Injekt.get(),
            getAnimeById: GetAnime = Injekt.get(),
            sourceManager: AnimeSourceManager = Injekt.get(),
        ): AnimeDownload? {
            val episode = getEpisode.await(chapterId) ?: return null
            val anime = getAnimeById.await(episode.animeId) ?: return null
            val source = sourceManager.get(anime.source) as? AnimeHttpSource ?: return null

            return AnimeDownload(source, anime, episode)
        }
    }
}
