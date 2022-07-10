package eu.kanade.domain.episode.interactor

import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.repository.EpisodeRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class GetEpisode(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(id: Long): Episode? {
        return try {
            episodeRepository.getEpisodeById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun await(url: String, animeId: Long): Episode? {
        return try {
            episodeRepository.getEpisodeByUrlAndAnimeId(url, animeId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }
}
