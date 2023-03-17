package eu.kanade.domain.items.episode.interactor

import eu.kanade.domain.items.episode.model.EpisodeUpdate
import eu.kanade.domain.items.episode.repository.EpisodeRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class UpdateEpisode(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(episodeUpdate: EpisodeUpdate) {
        try {
            episodeRepository.updateEpisode(episodeUpdate)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitAll(episodeUpdates: List<EpisodeUpdate>) {
        try {
            episodeRepository.updateAllEpisodes(episodeUpdates)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
