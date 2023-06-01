package tachiyomi.domain.items.episode.interactor

import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.repository.EpisodeRepository

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
