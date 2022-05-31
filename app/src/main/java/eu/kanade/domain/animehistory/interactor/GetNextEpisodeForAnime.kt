package eu.kanade.domain.animehistory.interactor

import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository
import eu.kanade.domain.episode.model.Episode

class GetNextEpisodeForAnime(
    private val repository: AnimeHistoryRepository,
) {

    suspend fun await(animeId: Long, episodeId: Long): Episode? {
        return repository.getNextEpisodeForAnime(animeId, episodeId)
    }
}
