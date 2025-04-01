package tachiyomi.domain.source.anime.interactor

import tachiyomi.domain.source.anime.repository.FeedSavedSearchRepository

class CountFeedSavedSearchGlobal(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(): Long {
        return feedSavedSearchRepository.countGlobal()
    }
}
