package eu.kanade.domain.episode.interactor

import eu.kanade.domain.episode.model.EpisodeUpdate
import eu.kanade.domain.episode.repository.EpisodeRepository

class UpdateEpisode(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(episodeUpdate: EpisodeUpdate) {
        episodeRepository.update(episodeUpdate)
    }
}
