package eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.api.get

class GlobalAnimeSearchScreenModel(
    initialQuery: String = "",
    initialExtensionFilter: String? = null,
) : AnimeSearchScreenModel<GlobalAnimeSearchScreenModel.State>(
    State(
        searchQuery = initialQuery,
    ),
) {

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || !initialExtensionFilter.isNullOrBlank()) {
            search(initialQuery)
        }
    }

    override fun getEnabledSources(): List<AnimeCatalogueSource> {
        return super.getEnabledSources()
            .filter { mutableState.value.sourceFilter != AnimeSourceFilter.PinnedOnly || "${it.id}" in pinnedSources }
    }

    override fun updateSearchQuery(query: String?) {
        mutableState.update {
            it.copy(searchQuery = query)
        }
    }

    override fun updateItems(items: Map<AnimeCatalogueSource, AnimeSearchItemResult>) {
        mutableState.update {
            it.copy(items = items)
        }
    }

    override fun getItems(): Map<AnimeCatalogueSource, AnimeSearchItemResult> {
        return mutableState.value.items
    }

    override fun setSourceFilter(filter: AnimeSourceFilter) {
        mutableState.update { it.copy(sourceFilter = filter) }
    }

    override fun toggleFilterResults() {
        mutableState.update {
            it.copy(onlyShowHasResults = !it.onlyShowHasResults)
        }
    }

    @Immutable
    data class State(
        val searchQuery: String? = null,
        val sourceFilter: AnimeSourceFilter = AnimeSourceFilter.PinnedOnly,
        val onlyShowHasResults: Boolean = false,
        val items: Map<AnimeCatalogueSource, AnimeSearchItemResult> = emptyMap(),
    ) {
        val progress: Int = items.count { it.value !is AnimeSearchItemResult.Loading }
        val total: Int = items.size
        val filteredItems = items.filter { (_, result) -> result.isVisible(onlyShowHasResults) }
    }
}
