package tachiyomi.domain.source.anime.interactor

import tachiyomi.domain.source.anime.repository.FeedSavedSearchRepository

class CountFeedSavedSearchBySourceId(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(sourceId: Long): Long {
        return feedSavedSearchRepository.countBySourceId(sourceId)
    }
}
