package tachiyomi.domain.source.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.anime.model.FeedSavedSearch
import tachiyomi.domain.source.anime.model.FeedSavedSearchUpdate
import tachiyomi.domain.source.anime.model.SavedSearch

interface FeedSavedSearchRepository {

    suspend fun getGlobal(): List<FeedSavedSearch>

    fun getGlobalAsFlow(): Flow<List<FeedSavedSearch>>

    suspend fun getGlobalFeedSavedSearch(): List<SavedSearch>

    suspend fun countGlobal(): Long

    suspend fun getBySourceId(sourceId: Long): List<FeedSavedSearch>

    fun getBySourceIdAsFlow(sourceId: Long): Flow<List<FeedSavedSearch>>

    suspend fun getBySourceIdFeedSavedSearch(sourceId: Long): List<SavedSearch>

    suspend fun countBySourceId(sourceId: Long): Long

    suspend fun delete(feedSavedSearchId: Long)

    suspend fun insert(feedSavedSearch: FeedSavedSearch): Long?

    suspend fun insertAll(feedSavedSearch: List<FeedSavedSearch>)

    // KMK -->
    suspend fun updatePartial(update: FeedSavedSearchUpdate)

    suspend fun updatePartial(updates: List<FeedSavedSearchUpdate>)
    // KMK <--
}
