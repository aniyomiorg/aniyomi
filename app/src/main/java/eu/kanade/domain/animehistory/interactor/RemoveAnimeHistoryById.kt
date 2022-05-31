package eu.kanade.domain.animehistory.interactor

import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository

class RemoveAnimeHistoryById(
    private val repository: AnimeHistoryRepository,
) {

    suspend fun await(history: AnimeHistoryWithRelations) {
        repository.resetHistory(history.id)
    }
}
