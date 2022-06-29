package eu.kanade.domain.animetrack.interactor

import eu.kanade.domain.animetrack.repository.AnimeTrackRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class DeleteAnimeTrack(
    private val trackRepository: AnimeTrackRepository,
) {

    suspend fun await(animeId: Long, syncId: Long) {
        try {
            trackRepository.delete(animeId, syncId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
