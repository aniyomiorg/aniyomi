package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack

/**
 * For track services api that support deleting a manga entry for a user's list
 */
interface DeletableMangaTrackService {

    suspend fun delete(track: MangaTrack): MangaTrack
}
