package eu.kanade.domain.history.manga.interactor

import eu.kanade.domain.history.manga.model.MangaHistoryUpdate
import eu.kanade.domain.history.manga.repository.MangaHistoryRepository

class UpsertMangaHistory(
    private val historyRepository: MangaHistoryRepository,
) {

    suspend fun await(historyUpdate: MangaHistoryUpdate) {
        historyRepository.upsertMangaHistory(historyUpdate)
    }
}
