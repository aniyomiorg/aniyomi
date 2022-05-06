package tachiyomi.animesource

import tachiyomi.animesource.model.AnimeInfo
import tachiyomi.animesource.model.EpisodeInfo
import tachiyomi.animesource.model.Video

/**
 * A basic interface for creating a source. It could be an online source, a local source, etc...
 */
interface AnimeSource {

    /**
     * Id for the source. Must be unique.
     */
    val id: Long

    /**
     * Name of the source.
     */
    val name: String

    // TODO remove CatalogSource?
    val lang: String

    /**
     * Returns an observable with the updated details for a anime.
     *
     * @param anime the anime to update.
     */
    suspend fun getAnimeDetails(anime: AnimeInfo): AnimeInfo

    /**
     * Returns an observable with all the available chapters for a anime.
     *
     * @param anime the anime to update.
     */
    suspend fun getEpisodeList(anime: AnimeInfo): List<EpisodeInfo>

    /**
     * Returns an observable with all the available chapters for a anime.
     *
     * @param anime the anime to update.
     */
    suspend fun getVideoList(episode: EpisodeInfo): List<Video>

    /**
     * Returns a regex used to determine chapter information.
     *
     * @return empty regex will run default parser.
     */
    fun getRegex(): Regex {
        return Regex("")
    }
}
