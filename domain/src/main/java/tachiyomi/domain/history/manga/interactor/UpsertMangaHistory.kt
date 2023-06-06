package tachiyomi.domain.history.manga.interactor

import tachiyomi.domain.history.manga.model.MangaHistoryUpdate
import tachiyomi.domain.history.manga.repository.MangaHistoryRepository

class UpsertMangaHistory(
    private val historyRepository: MangaHistoryRepository,
) {

    suspend fun await(historyUpdate: MangaHistoryUpdate) {
        historyRepository.upsertMangaHistory(historyUpdate)
    }
}
