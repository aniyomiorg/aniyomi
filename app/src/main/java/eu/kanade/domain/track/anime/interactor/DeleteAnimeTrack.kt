package eu.kanade.domain.track.anime.interactor

import eu.kanade.domain.track.anime.repository.AnimeTrackRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class DeleteAnimeTrack(
    private val trackRepository: AnimeTrackRepository,
) {

    suspend fun await(animeId: Long, syncId: Long) {
        try {
            trackRepository.deleteAnime(animeId, syncId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
