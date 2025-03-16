package tachiyomi.domain.source.anime.interactor

import logcat.LogPriority
import logcat.asLog
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.anime.model.FeedSavedSearch
import tachiyomi.domain.source.anime.repository.FeedSavedSearchRepository

class InsertFeedSavedSearch(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(feedSavedSearch: FeedSavedSearch): Long? {
        return try {
            feedSavedSearchRepository.insert(feedSavedSearch)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.asLog() }
            null
        }
    }

    suspend fun awaitAll(feedSavedSearch: List<FeedSavedSearch>) {
        try {
            feedSavedSearchRepository.insertAll(feedSavedSearch)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.asLog() }
        }
    }
}
