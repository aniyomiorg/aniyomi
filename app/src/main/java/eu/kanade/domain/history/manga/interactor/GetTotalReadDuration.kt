package eu.kanade.domain.history.manga.interactor

import eu.kanade.domain.history.manga.repository.MangaHistoryRepository

class GetTotalReadDuration(
    private val repository: MangaHistoryRepository,
) {

    suspend fun await(): Long {
        return repository.getTotalReadDuration()
    }
}
