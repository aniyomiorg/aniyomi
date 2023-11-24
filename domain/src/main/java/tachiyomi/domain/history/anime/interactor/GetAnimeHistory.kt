package tachiyomi.domain.history.anime.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.anime.model.AnimeHistory
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository

class GetAnimeHistory(
    private val repository: AnimeHistoryRepository,
) {

    suspend fun await(animeId: Long): List<AnimeHistory> {
        return repository.getHistoryByAnimeId(animeId)
    }

    fun subscribe(query: String): Flow<List<AnimeHistoryWithRelations>> {
        return repository.getAnimeHistory(query)
    }
}
