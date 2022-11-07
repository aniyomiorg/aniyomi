package eu.kanade.tachiyomi.data.animedownload.model

import eu.kanade.domain.anime.interactor.GetAnime
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.episode.interactor.GetEpisode
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.models.Episode
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
    private var statusSubject: PublishSubject<AnimeDownload>? = null

    @Transient
    private var progressSubject: PublishSubject<AnimeDownload>? = null

    @Transient
    private var statusCallback: ((AnimeDownload) -> Unit)? = null

    @Transient
    private var progressCallback: ((AnimeDownload) -> Unit)? = null

    val progress: Int
        get() {
            val video = video ?: return 0
            return video.progress
        }

    fun setStatusSubject(subject: PublishSubject<AnimeDownload>?) {
        statusSubject = subject
    }

    fun setProgressSubject(subject: PublishSubject<AnimeDownload>?) {
        progressSubject = subject
    }

    fun setStatusCallback(f: ((AnimeDownload) -> Unit)?) {
        statusCallback = f
    }

    fun setProgressCallback(f: ((AnimeDownload) -> Unit)?) {
        progressCallback = f
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

            return AnimeDownload(source, anime, episode.toDbEpisode())
        }
    }
}
