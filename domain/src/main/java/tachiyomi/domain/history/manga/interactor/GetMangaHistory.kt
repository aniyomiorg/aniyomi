package tachiyomi.domain.history.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.manga.model.MangaHistory
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.domain.history.manga.repository.MangaHistoryRepository

class GetMangaHistory(
    private val repository: MangaHistoryRepository,
) {

    suspend fun await(mangaId: Long): List<MangaHistory> {
        return repository.getHistoryByMangaId(mangaId)
    }

    fun subscribe(query: String): Flow<List<MangaHistoryWithRelations>> {
        return repository.getMangaHistory(query)
    }
}
