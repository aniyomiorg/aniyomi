package eu.kanade.domain.history.anime.interactor

import eu.kanade.domain.history.anime.repository.AnimeHistoryRepository
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations

class RemoveAnimeHistory(
    private val repository: AnimeHistoryRepository,
) {

    suspend fun awaitAll(): Boolean {
        return repository.deleteAllAnimeHistory()
    }

    suspend fun await(history: AnimeHistoryWithRelations) {
        repository.resetAnimeHistory(history.id)
    }

    suspend fun await(animeId: Long) {
        repository.resetHistoryByAnimeId(animeId)
    }
}
