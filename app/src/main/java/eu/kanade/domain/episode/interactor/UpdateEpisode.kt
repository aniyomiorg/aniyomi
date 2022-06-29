package eu.kanade.domain.episode.interactor

import eu.kanade.domain.episode.model.EpisodeUpdate
import eu.kanade.domain.episode.repository.EpisodeRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class UpdateEpisode(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(episodeUpdate: EpisodeUpdate) {
        try {
            episodeRepository.update(episodeUpdate)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitAll(episodeUpdates: List<EpisodeUpdate>) {
        try {
            episodeRepository.updateAll(episodeUpdates)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
