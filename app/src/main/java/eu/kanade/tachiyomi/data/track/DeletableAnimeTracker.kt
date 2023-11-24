package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack

/**
 *Tracker that support deleting am entry from a user's list
 */
interface DeletableAnimeTracker {

    suspend fun delete(track: AnimeTrack): AnimeTrack
}
