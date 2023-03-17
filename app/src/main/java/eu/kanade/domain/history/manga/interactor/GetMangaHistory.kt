package eu.kanade.domain.history.manga.interactor

import eu.kanade.domain.history.manga.model.MangaHistoryWithRelations
import eu.kanade.domain.history.manga.repository.MangaHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetMangaHistory(
    private val repository: MangaHistoryRepository,
) {

    fun subscribe(query: String): Flow<List<MangaHistoryWithRelations>> {
        return repository.getMangaHistory(query)
    }
}
