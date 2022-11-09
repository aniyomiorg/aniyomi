package eu.kanade.domain.animehistory.interactor

import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository

class RemoveAnimeHistory(
    private val repository: AnimeHistoryRepository,
) {

    suspend fun awaitAll(): Boolean {
        return repository.deleteAllHistory()
    }

    suspend fun await(history: AnimeHistoryWithRelations) {
        repository.resetHistory(history.id)
    }

    suspend fun await(animeId: Long) {
        repository.resetHistoryByAnimeId(animeId)
    }
}
