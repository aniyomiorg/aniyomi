package eu.kanade.domain.episode.interactor

import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.repository.EpisodeRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class GetEpisodeByAnimeId(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(animeId: Long): List<Episode> {
        return try {
            episodeRepository.getEpisodeByAnimeId(animeId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
