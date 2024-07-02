package tachiyomi.domain.track.anime.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.anime.repository.AnimeTrackRepository

class DeleteAnimeTrack(
    private val trackRepository: AnimeTrackRepository,
) {

    suspend fun await(animeId: Long, trackerId: Long) {
        try {
            trackRepository.delete(animeId, trackerId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
