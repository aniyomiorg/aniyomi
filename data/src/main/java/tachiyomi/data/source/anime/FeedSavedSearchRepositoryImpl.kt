package tachiyomi.data.source.anime

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.source.anime.model.FeedSavedSearch
import tachiyomi.domain.source.anime.model.FeedSavedSearchUpdate
import tachiyomi.domain.source.anime.model.SavedSearch
import tachiyomi.domain.source.anime.repository.FeedSavedSearchRepository
import tachiyomi.mi.data.AnimeDatabase

class FeedSavedSearchRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : FeedSavedSearchRepository {

    override suspend fun getGlobal(): List<FeedSavedSearch> {
        return handler.awaitList {
            feed_saved_searchQueries.selectAllGlobal(FeedSavedSearchMapper::map)
        }
    }

    override fun getGlobalAsFlow(): Flow<List<FeedSavedSearch>> {
        return handler.subscribeToList {
            feed_saved_searchQueries.selectAllGlobal(FeedSavedSearchMapper::map)
        }
    }

    override suspend fun getGlobalFeedSavedSearch(): List<SavedSearch> {
        return handler.awaitList { feed_saved_searchQueries.selectGlobalFeedSavedSearch(SavedSearchMapper::map) }
    }

    override suspend fun countGlobal(): Long {
        return handler.awaitOne { feed_saved_searchQueries.countGlobal() }
    }

    override suspend fun getBySourceId(sourceId: Long): List<FeedSavedSearch> {
        return handler.awaitList {
            feed_saved_searchQueries.selectBySource(sourceId, FeedSavedSearchMapper::map)
        }
    }

    override fun getBySourceIdAsFlow(sourceId: Long): Flow<List<FeedSavedSearch>> {
        return handler.subscribeToList {
            feed_saved_searchQueries.selectBySource(sourceId, FeedSavedSearchMapper::map)
        }
    }

    override suspend fun getBySourceIdFeedSavedSearch(sourceId: Long): List<SavedSearch> {
        return handler.awaitList {
            feed_saved_searchQueries.selectSourceFeedSavedSearch(
                sourceId,
                SavedSearchMapper::map,
            )
        }
    }

    override suspend fun countBySourceId(sourceId: Long): Long {
        return handler.awaitOne { feed_saved_searchQueries.countSourceFeedSavedSearch(sourceId) }
    }

    override suspend fun delete(feedSavedSearchId: Long) {
        handler.await { feed_saved_searchQueries.deleteById(feedSavedSearchId) }
    }

    override suspend fun insert(feedSavedSearch: FeedSavedSearch): Long {
        // KMK -->
        return handler.await(true) {
            val currentFeeds = handler.awaitList {
                feed_saved_searchQueries.selectAll(FeedSavedSearchMapper::map)
            }
            val existedFeedId = currentFeeds.find { currentFeed ->
                currentFeed.source == feedSavedSearch.source &&
                    currentFeed.savedSearch == feedSavedSearch.savedSearch &&
                    currentFeed.global == feedSavedSearch.global
            }?.id

            existedFeedId
                // KMK <--
                ?: handler.awaitOneExecutable(true) {
                    feed_saved_searchQueries.insert(
                        feedSavedSearch.source,
                        feedSavedSearch.savedSearch,
                        feedSavedSearch.global,
                    )
                    feed_saved_searchQueries.selectLastInsertedRowId()
                }
        }
    }

    override suspend fun insertAll(feedSavedSearch: List<FeedSavedSearch>) {
        return handler.await(true) {
            feedSavedSearch.forEach {
                feed_saved_searchQueries.insert(
                    it.source,
                    it.savedSearch,
                    it.global,
                )
            }
        }
    }

    // KMK -->
    override suspend fun updatePartial(update: FeedSavedSearchUpdate) {
        handler.await {
            updatePartialBlocking(update)
        }
    }

    override suspend fun updatePartial(updates: List<FeedSavedSearchUpdate>) {
        handler.await(inTransaction = true) {
            for (update in updates) {
                updatePartialBlocking(update)
            }
        }
    }

    private fun AnimeDatabase.updatePartialBlocking(update: FeedSavedSearchUpdate) {
        feed_saved_searchQueries.update(
            source = update.source,
            saved_search = update.savedSearch,
            global = update.global,
            feed_order = update.feedOrder,
            id = update.id,
        )
    }
    // KMK <--
}
