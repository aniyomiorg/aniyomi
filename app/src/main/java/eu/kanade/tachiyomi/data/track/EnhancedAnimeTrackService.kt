package eu.kanade.tachiyomi.data.track

import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.domain.track.anime.model.AnimeTrack
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch

/**
 * An Enhanced Track Service will never prompt the user to match a manga with the remote.
 * It is expected that such Track Service can only work with specific sources and unique IDs.
 */
interface EnhancedAnimeTrackService {
    /**
     * This TrackService will only work with the sources that are accepted by this filter function.
     */
    fun accept(source: AnimeSource): Boolean {
        return source::class.qualifiedName in getAcceptedSources()
    }

    /**
     * Fully qualified source classes that this track service is compatible with.
     */
    fun getAcceptedSources(): List<String>

    /**
     * match is similar to TrackService.search, but only return zero or one match.
     */
    suspend fun match(anime: Anime): AnimeTrackSearch?

    /**
     * Checks whether the provided source/track/anime triplet is from this AnimeTrackService
     */
    fun isTrackFrom(track: AnimeTrack, anime: Anime, source: AnimeSource?): Boolean

    /**
     * Migrates the given track for the anime to the newSource, if possible
     */
    fun migrateTrack(track: AnimeTrack, anime: Anime, newSource: AnimeSource): AnimeTrack?
}
