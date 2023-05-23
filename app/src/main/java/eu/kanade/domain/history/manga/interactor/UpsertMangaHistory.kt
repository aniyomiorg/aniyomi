package eu.kanade.domain.history.manga.interactor

import eu.kanade.domain.history.manga.repository.MangaHistoryRepository
import tachiyomi.domain.history.manga.model.MangaHistoryUpdate

class UpsertMangaHistory(
    private val historyRepository: MangaHistoryRepository,
) {

    suspend fun await(historyUpdate: MangaHistoryUpdate) {
        historyRepository.upsertMangaHistory(historyUpdate)
    }
}
