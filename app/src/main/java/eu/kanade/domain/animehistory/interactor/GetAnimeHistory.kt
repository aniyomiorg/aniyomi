package eu.kanade.domain.animehistory.interactor

import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetAnimeHistory(
    private val repository: AnimeHistoryRepository,
) {

    fun subscribe(query: String): Flow<List<AnimeHistoryWithRelations>> {
        return repository.getHistory(query)
    }
}
