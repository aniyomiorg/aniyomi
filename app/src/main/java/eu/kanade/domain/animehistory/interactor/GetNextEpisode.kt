package eu.kanade.domain.animehistory.interactor

import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository
import eu.kanade.domain.episode.model.Episode

class GetNextEpisode(
    private val repository: AnimeHistoryRepository,
) {

    suspend fun await(animeId: Long, episodeId: Long): Episode? {
        return repository.getNextEpisode(animeId, episodeId)
    }

    suspend fun await(): Episode? {
        val history = repository.getLastHistory() ?: return null
        return repository.getNextEpisode(history.animeId, history.episodeId)
    }
}
