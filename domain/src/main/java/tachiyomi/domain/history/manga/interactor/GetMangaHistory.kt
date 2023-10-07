package tachiyomi.domain.history.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.domain.history.manga.repository.MangaHistoryRepository

class GetMangaHistory(
    private val repository: MangaHistoryRepository,
) {

    fun subscribe(query: String): Flow<List<MangaHistoryWithRelations>> {
        return repository.getMangaHistory(query)
    }
}
