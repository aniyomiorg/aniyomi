package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.source.AnimeSource

/**
 * An Unattended Track Service will never prompt the user to match a manga with the remote.
 * It is expected that such Track Sercice can only work with specific sources and unique IDs.
 */
interface UnattendedTrackService {
    /**
     * This TrackService will only work with the sources that are accepted by this filter function.
     */
    fun accept(source: Source): Boolean

    /**
     * This TrackService will only work with the sources that are accepted by this filter function.
     */
    fun accept(source: AnimeSource): Boolean

    /**
     * match is similar to TrackService.search, but only return zero or one match.
     */
    suspend fun match(manga: Manga): TrackSearch?

    /**
     * match is similar to TrackService.search, but only return zero or one match.
     */
    suspend fun match(anime: Anime): AnimeTrackSearch?
}
