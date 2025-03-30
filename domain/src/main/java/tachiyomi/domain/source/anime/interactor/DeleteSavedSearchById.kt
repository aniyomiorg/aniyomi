package tachiyomi.domain.source.anime.interactor

import tachiyomi.domain.source.anime.repository.SavedSearchRepository

class DeleteSavedSearchById(
    private val savedSearchRepository: SavedSearchRepository,
) {

    suspend fun await(savedSearchId: Long) {
        return savedSearchRepository.delete(savedSearchId)
    }
}
