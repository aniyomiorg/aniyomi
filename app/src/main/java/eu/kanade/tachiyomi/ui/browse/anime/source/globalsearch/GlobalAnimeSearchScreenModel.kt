package eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch

import androidx.compose.runtime.Immutable
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class GlobalAnimeSearchScreenModel(
    initialQuery: String = "",
    initialExtensionFilter: String = "",
    preferences: BasePreferences = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
) : AnimeSearchScreenModel<GlobalAnimeSearchScreenModel.State>(
    State(
        searchQuery = initialQuery,
    ),
) {

    val incognitoMode = preferences.incognitoMode()
    val lastUsedSourceId = sourcePreferences.lastUsedAnimeSource()

    val searchPagerFlow = state.map { Pair(it.onlyShowHasResults, it.items) }
        .distinctUntilChanged()
        .map { (onlyShowHasResults, items) ->
            items.filter { (_, result) -> result.isVisible(onlyShowHasResults) }
        }
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, state.value.items)

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || initialExtensionFilter.isNotBlank()) {
            search(initialQuery)
        }
    }

    override fun getEnabledSources(): List<AnimeCatalogueSource> {
        val enabledLanguages = sourcePreferences.enabledLanguages().get()
        val disabledSources = sourcePreferences.disabledAnimeSources().get()
        val pinnedSources = sourcePreferences.pinnedAnimeSources().get()

        return sourceManager.getCatalogueSources()
            .filter { mutableState.value.sourceFilter != AnimeSourceFilter.PinnedOnly || "${it.id}" in pinnedSources }
            .filter { it.lang in enabledLanguages }
            .filterNot { "${it.id}" in disabledSources }
            .sortedWith(compareBy({ "${it.id}" !in pinnedSources }, { "${it.name.lowercase()} (${it.lang})" }))
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

    fun setSourceFilter(filter: AnimeSourceFilter) {
        mutableState.update { it.copy(sourceFilter = filter) }
    }

    fun toggleFilterResults() {
        mutableState.update {
            it.copy(onlyShowHasResults = !it.onlyShowHasResults)
        }
    }

    private fun AnimeSearchItemResult.isVisible(onlyShowHasResults: Boolean): Boolean {
        return !onlyShowHasResults || (this is AnimeSearchItemResult.Success && !this.isEmpty)
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
    }
}
