package eu.kanade.domain.history.anime.interactor

import eu.kanade.domain.history.anime.model.AnimeHistoryUpdate
import eu.kanade.domain.history.anime.repository.AnimeHistoryRepository

class UpsertAnimeHistory(
    private val historyRepository: AnimeHistoryRepository,
) {

    suspend fun await(historyUpdate: AnimeHistoryUpdate) {
        historyRepository.upsertAnimeHistory(historyUpdate)
    }
}
