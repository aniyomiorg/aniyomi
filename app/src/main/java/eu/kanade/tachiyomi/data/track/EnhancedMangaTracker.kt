package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.source.MangaSource
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.track.manga.model.MangaTrack

/**
 * A tracker that will never prompt the user to manually bind an entry.
 * It is expected that such tracker can only work with specific sources and unique IDs.
 */
interface EnhancedMangaTracker {
    /**
     * This Tracker will only work with the sources that are accepted by this filter function.
     */
    fun accept(source: MangaSource): Boolean {
        return source::class.qualifiedName in getAcceptedSources()
    }

    /**
     * Fully qualified source classes that this track service is compatible with.
     */
    fun getAcceptedSources(): List<String>

    fun loginNoop()

    /**
     * match is similar to Tracker.search, but only return zero or one match.
     */
    suspend fun match(manga: Manga): MangaTrackSearch?

    /**
     * Checks whether the provided source/track/manga triplet is from this [MangaTrack]
     */
    fun isTrackFrom(track: MangaTrack, manga: Manga, source: MangaSource?): Boolean

    /**
     * Migrates the given track for the manga to the newSource, if possible
     */
    fun migrateTrack(track: MangaTrack, manga: Manga, newSource: MangaSource): MangaTrack?
}
