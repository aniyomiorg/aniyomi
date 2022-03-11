package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch

/**
 * An Enhanced Track Service will never prompt the user to match a manga with the remote.
 * It is expected that such Track Service can only work with specific sources and unique IDs.
 */
interface EnhancedTrackService {
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
}
