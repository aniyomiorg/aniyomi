package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack

/**
 * Tracker that support deleting am entry from a user's list
 */
interface DeletableMangaTracker {

    suspend fun delete(track: MangaTrack): MangaTrack
}
