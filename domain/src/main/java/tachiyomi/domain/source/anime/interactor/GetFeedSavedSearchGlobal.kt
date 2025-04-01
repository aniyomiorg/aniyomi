package tachiyomi.domain.source.anime.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.anime.model.FeedSavedSearch
import tachiyomi.domain.source.anime.repository.FeedSavedSearchRepository

class GetFeedSavedSearchGlobal(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(): List<FeedSavedSearch> {
        return feedSavedSearchRepository.getGlobal()
    }

    fun subscribe(): Flow<List<FeedSavedSearch>> {
        return feedSavedSearchRepository.getGlobalAsFlow()
    }
}
