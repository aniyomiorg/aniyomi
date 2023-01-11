package eu.kanade.tachiyomi.data.track

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.animetrack.model.AnimeTrack
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.domain.manga.model.Manga
import eu.kanade.domain.track.model.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.Source

/**
 * An Enhanced Track Service will never prompt the user to match a manga with the remote.
 * It is expected that such Track Service can only work with specific sources and unique IDs.
 */
interface EnhancedTrackService {
    /**
     * This TrackService will only work with the sources that are accepted by this filter function.
     */
    fun accept(source: Source): Boolean {
        return source::class.qualifiedName in getAcceptedSources()
    }

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
    suspend fun match(manga: Manga): TrackSearch?

    /**
     * match is similar to TrackService.search, but only return zero or one match.
     */
    suspend fun match(anime: Anime): AnimeTrackSearch?

    /**
     * Checks whether the provided source/track/manga triplet is from this TrackService
     */
    fun isTrackFrom(track: Track, manga: Manga, source: Source?): Boolean

    /**
     * Checks whether the provided source/track/anime triplet is from this AnimeTrackService
     */
    fun isTrackFrom(track: AnimeTrack, anime: DomainAnime, source: AnimeSource?): Boolean

    /**
     * Migrates the given track for the manga to the newSource, if possible
     */
    fun migrateTrack(track: Track, manga: Manga, newSource: Source): Track?

    /**
     * Migrates the given track for the anime to the newSource, if possible
     */
    fun migrateTrack(track: AnimeTrack, anime: Anime, newSource: AnimeSource): AnimeTrack?
}
