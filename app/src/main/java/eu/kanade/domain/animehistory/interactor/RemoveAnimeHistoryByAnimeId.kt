package eu.kanade.domain.animehistory.interactor

import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository

class RemoveAnimeHistoryByAnimeId(
    private val repository: AnimeHistoryRepository
) {

    suspend fun await(animeId: Long) {
        repository.resetHistoryByAnimeId(animeId)
    }
}
