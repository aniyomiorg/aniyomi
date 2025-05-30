package tachiyomi.domain.source.anime.interactor

import tachiyomi.domain.source.anime.model.SavedSearch
import tachiyomi.domain.source.anime.repository.SavedSearchRepository

class GetSavedSearchById(
    private val savedSearchRepository: SavedSearchRepository,
) {

    suspend fun await(savedSearchId: Long): SavedSearch {
        return savedSearchRepository.getById(savedSearchId)!!
    }

    suspend fun awaitOrNull(savedSearchId: Long): SavedSearch? {
        return savedSearchRepository.getById(savedSearchId)
    }
}
