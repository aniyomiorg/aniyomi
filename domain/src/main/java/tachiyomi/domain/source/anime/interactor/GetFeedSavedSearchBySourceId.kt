package tachiyomi.domain.source.anime.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.anime.model.FeedSavedSearch
import tachiyomi.domain.source.anime.repository.FeedSavedSearchRepository

class GetFeedSavedSearchBySourceId(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(sourceId: Long): List<FeedSavedSearch> {
        return feedSavedSearchRepository.getBySourceId(sourceId)
    }

    fun subscribe(sourceId: Long): Flow<List<FeedSavedSearch>> {
        return feedSavedSearchRepository.getBySourceIdAsFlow(sourceId)
    }
}
