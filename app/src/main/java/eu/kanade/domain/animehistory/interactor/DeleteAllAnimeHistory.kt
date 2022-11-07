package eu.kanade.domain.animehistory.interactor

import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository

class DeleteAllAnimeHistory(
    private val repository: AnimeHistoryRepository,
) {

    suspend fun await(): Boolean {
        return repository.deleteAllHistory()
    }
}
