package eu.kanade.domain.animetrack.interactor

import eu.kanade.domain.animetrack.model.AnimeTrack
import eu.kanade.domain.animetrack.repository.AnimeTrackRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class InsertAnimeTrack(
    private val animetrackRepository: AnimeTrackRepository,
) {

    suspend fun await(track: AnimeTrack) {
        try {
            animetrackRepository.insert(track)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitAll(tracks: List<AnimeTrack>) {
        try {
            animetrackRepository.insertAll(tracks)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
