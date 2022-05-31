package eu.kanade.domain.animehistory.interactor

import eu.kanade.domain.animehistory.model.AnimeHistoryUpdate
import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository

class UpsertAnimeHistory(
    private val animehistoryRepository: AnimeHistoryRepository,
) {

    suspend fun await(historyUpdate: AnimeHistoryUpdate) {
        animehistoryRepository.upsertHistory(historyUpdate)
    }
}
