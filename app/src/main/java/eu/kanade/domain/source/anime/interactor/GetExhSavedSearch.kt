package eu.kanade.domain.source.anime.interactor

import eu.kanade.tachiyomi.animesource.model.FilterList
import exh.util.nullIfBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.source.anime.interactor.GetSavedSearchById
import tachiyomi.domain.source.anime.interactor.GetSavedSearchBySourceId
import tachiyomi.domain.source.anime.model.EXHSavedSearch
import tachiyomi.domain.source.anime.model.SavedSearch
import xyz.nulldev.ts.api.http.serializer.FilterSerializer

class GetExhSavedSearch(
    private val getSavedSearchById: GetSavedSearchById,
    private val getSavedSearchBySourceId: GetSavedSearchBySourceId,
    private val filterSerializer: FilterSerializer,
) {

    suspend fun awaitOne(savedSearchId: Long, getFilterList: () -> FilterList): EXHSavedSearch? {
        val search = getSavedSearchById.awaitOrNull(savedSearchId) ?: return null
        return withIOContext { loadSearch(search, getFilterList) }
    }

    suspend fun await(sourceId: Long, getFilterList: () -> FilterList): List<EXHSavedSearch> {
        return withIOContext { loadSearches(getSavedSearchBySourceId.await(sourceId), getFilterList) }
    }

    fun subscribe(sourceId: Long, getFilterList: () -> FilterList): Flow<List<EXHSavedSearch>> {
        return getSavedSearchBySourceId.subscribe(sourceId)
            .map { loadSearches(it, getFilterList) }
            .flowOn(Dispatchers.IO)
    }

    private fun loadSearches(searches: List<SavedSearch>, getFilterList: () -> FilterList): List<EXHSavedSearch> {
        return searches.map { loadSearch(it, getFilterList) }
    }

    private fun loadSearch(search: SavedSearch, getFilterList: () -> FilterList): EXHSavedSearch {
        val originalFilters = getFilterList()
        val filters = getFilters(search.filtersJson)

        return EXHSavedSearch(
            id = search.id,
            name = search.name,
            query = search.query?.nullIfBlank(),
            filterList = filters?.let { deserializeFilters(it, originalFilters) },
        )
    }

    private fun getFilters(filtersJson: String?): JsonArray? {
        return runCatching {
            filtersJson?.let { Json.decodeFromString<JsonArray>(it) }
        }.onFailure {
        }.getOrNull()
    }

    private fun deserializeFilters(filters: JsonArray, originalFilters: FilterList): FilterList? {
        return runCatching {
            filterSerializer.deserialize(originalFilters, filters)
            originalFilters
        }.onFailure {
        }.getOrNull()
    }
}
