package eu.kanade.domain.episode.repository

import eu.kanade.domain.episode.model.EpisodeUpdate

interface EpisodeRepository {

    suspend fun update(episodeUpdate: EpisodeUpdate)
}
