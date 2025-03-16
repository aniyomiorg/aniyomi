package tachiyomi.domain.source.anime.interactor

import tachiyomi.domain.source.anime.repository.FeedSavedSearchRepository

class DeleteFeedSavedSearchById(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(feedSavedSearchId: Long) {
        return feedSavedSearchRepository.delete(feedSavedSearchId)
    }
}
