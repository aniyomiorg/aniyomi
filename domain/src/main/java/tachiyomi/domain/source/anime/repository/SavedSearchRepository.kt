package tachiyomi.domain.source.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.anime.model.SavedSearch

interface SavedSearchRepository {

    suspend fun getById(savedSearchId: Long): SavedSearch?

    suspend fun getBySourceId(sourceId: Long): List<SavedSearch>

    fun getBySourceIdAsFlow(sourceId: Long): Flow<List<SavedSearch>>

    suspend fun delete(savedSearchId: Long)

    suspend fun insert(savedSearch: SavedSearch): Long?

    suspend fun insertAll(savedSearch: List<SavedSearch>)
}
