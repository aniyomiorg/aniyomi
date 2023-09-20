package tachiyomi.domain.history.manga.interactor

import tachiyomi.domain.history.manga.repository.MangaHistoryRepository

class GetTotalReadDuration(
    private val repository: MangaHistoryRepository,
) {

    suspend fun await(): Long {
        return repository.getTotalReadDuration()
    }
}
