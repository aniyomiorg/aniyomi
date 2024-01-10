package eu.kanade.tachiyomi.data.track

import tachiyomi.domain.track.manga.model.MangaTrack

/**
 * Tracker that support deleting am entry from a user's list
 */
interface DeletableMangaTracker {

    suspend fun delete(track: MangaTrack)
}
