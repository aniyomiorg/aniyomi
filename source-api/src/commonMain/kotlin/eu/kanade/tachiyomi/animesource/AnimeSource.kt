package eu.kanade.tachiyomi.animesource

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.util.awaitSingle
import rx.Observable

/**
 * A basic interface for creating a source. It could be an online source, a local source, etc.
 */
interface AnimeSource {

    /**
     * ID for the source. Must be unique.
     */
    val id: Long

    /**
     * Name of the source.
     */
    val name: String

    val lang: String
        get() = ""

    /**
     * Returns an observable with the updated details for a anime.
     *
     * @param anime the anime to update.
     */
    @Deprecated(
        "Use the 1.x API instead",
        ReplaceWith("getAnimeDetails"),
    )
    fun fetchAnimeDetails(anime: SAnime): Observable<SAnime> = throw IllegalStateException("Not used")

    /**
     * Returns an observable with all the available episodes for a anime.
     *
     * @param anime the anime to update.
     */
    @Deprecated(
        "Use the 1.x API instead",
        ReplaceWith("getEpisodeList"),
    )
    fun fetchEpisodeList(anime: SAnime): Observable<List<SEpisode>> = throw IllegalStateException("Not used")

    /**
     * Returns an observable with the list of videos a episode has. Videos should be returned
     * in the expected order; the index is ignored.
     *
     * @param episode the episode.
     */
    @Deprecated(
        "Use the 1.x API instead",
        ReplaceWith("getVideoList"),
    )
    fun fetchVideoList(episode: SEpisode): Observable<List<Video>> = Observable.empty()

    /**
     * [1.x API] Get the updated details for a anime.
     */
    @Suppress("DEPRECATION")
    suspend fun getAnimeDetails(anime: SAnime): SAnime {
        return fetchAnimeDetails(anime).awaitSingle()
    }

    /**
     * [1.x API] Get all the available episodes for a anime.
     */
    @Suppress("DEPRECATION")
    suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        return fetchEpisodeList(anime).awaitSingle()
    }

    /**
     * [1.x API] Get the list of videos a episode has. Videos should be returned
     * in the expected order; the index is ignored.
     */
    @Suppress("DEPRECATION")
    suspend fun getVideoList(episode: SEpisode): List<Video> {
        return fetchVideoList(episode).awaitSingle()
    }
}
