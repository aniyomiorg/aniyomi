package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack

/**
 * For track services api that support deleting a manga entry for a user's list
 */
interface DeletableAnimeTrackService {

    suspend fun delete(track: AnimeTrack): AnimeTrack
}
