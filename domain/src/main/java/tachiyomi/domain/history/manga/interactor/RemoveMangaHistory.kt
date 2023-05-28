package tachiyomi.domain.history.manga.interactor

import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.domain.history.manga.repository.MangaHistoryRepository

class RemoveMangaHistory(
    private val repository: MangaHistoryRepository,
) {

    suspend fun awaitAll(): Boolean {
        return repository.deleteAllMangaHistory()
    }

    suspend fun await(history: MangaHistoryWithRelations) {
        repository.resetMangaHistory(history.id)
    }

    suspend fun await(mangaId: Long) {
        repository.resetHistoryByMangaId(mangaId)
    }
}
