package eu.kanade.domain.history.anime.interactor

import eu.kanade.domain.history.anime.repository.AnimeHistoryRepository
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations

class GetAnimeHistory(
    private val repository: AnimeHistoryRepository,
) {

    fun subscribe(query: String): Flow<List<AnimeHistoryWithRelations>> {
        return repository.getAnimeHistory(query)
    }
}
