package tachiyomi.data.source.anime

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.source.anime.model.SavedSearch
import tachiyomi.domain.source.anime.repository.SavedSearchRepository

class SavedSearchRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : SavedSearchRepository {

    override suspend fun getById(savedSearchId: Long): SavedSearch? {
        return handler.awaitOneOrNull { saved_searchQueries.selectById(savedSearchId, SavedSearchMapper::map) }
    }

    override suspend fun getBySourceId(sourceId: Long): List<SavedSearch> {
        return handler.awaitList { saved_searchQueries.selectBySource(sourceId, SavedSearchMapper::map) }
    }

    override fun getBySourceIdAsFlow(sourceId: Long): Flow<List<SavedSearch>> {
        return handler.subscribeToList { saved_searchQueries.selectBySource(sourceId, SavedSearchMapper::map) }
    }

    override suspend fun delete(savedSearchId: Long) {
        handler.await { saved_searchQueries.deleteById(savedSearchId) }
    }

    override suspend fun insert(savedSearch: SavedSearch): Long {
        // KMK -->
        return handler.await(true) {
            val currentSavedSearches = handler.awaitList {
                saved_searchQueries.selectAll(SavedSearchMapper::map)
            }
            val existedSavedSearchId = currentSavedSearches.find { currentSavedSearch ->
                currentSavedSearch.source == savedSearch.source &&
                    currentSavedSearch.name == savedSearch.name &&
                    currentSavedSearch.query == savedSearch.query &&
                    currentSavedSearch.filtersJson == savedSearch.filtersJson
            }?.id

            existedSavedSearchId
                // KMK <--
                ?: handler.awaitOneExecutable(true) {
                    saved_searchQueries.insert(
                        savedSearch.source,
                        savedSearch.name,
                        savedSearch.query,
                        savedSearch.filtersJson,
                    )
                    saved_searchQueries.selectLastInsertedRowId()
                }
        }
    }

    override suspend fun insertAll(savedSearch: List<SavedSearch>) {
        handler.await(true) {
            savedSearch.forEach {
                saved_searchQueries.insert(
                    it.source,
                    it.name,
                    it.query,
                    it.filtersJson,
                )
            }
        }
    }
}
