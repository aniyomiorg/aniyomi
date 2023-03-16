package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import androidx.compose.runtime.Immutable
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.CatalogueMangaSource
import eu.kanade.tachiyomi.source.SourceManager
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class GlobalSearchScreenModel(
    initialQuery: String = "",
    initialExtensionFilter: String = "",
    preferences: BasePreferences = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) : SearchScreenModel<GlobalSearchState>(GlobalSearchState(searchQuery = initialQuery)) {

    val incognitoMode = preferences.incognitoMode()
    val lastUsedSourceId = sourcePreferences.lastUsedMangaSource()

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || initialExtensionFilter.isNotBlank()) {
            search(initialQuery)
        }
    }

    override fun getEnabledSources(): List<CatalogueMangaSource> {
        val enabledLanguages = sourcePreferences.enabledLanguages().get()
        val disabledSources = sourcePreferences.disabledMangaSources().get()
        val pinnedSources = sourcePreferences.pinnedMangaSources().get()

        return sourceManager.getCatalogueSources()
            .filter { it.lang in enabledLanguages }
            .filterNot { "${it.id}" in disabledSources }
            .sortedWith(compareBy({ "${it.id}" !in pinnedSources }, { "${it.name.lowercase()} (${it.lang})" }))
    }

    override fun updateSearchQuery(query: String?) {
        mutableState.update {
            it.copy(searchQuery = query)
        }
    }

    override fun updateItems(items: Map<CatalogueMangaSource, SearchItemResult>) {
        mutableState.update {
            it.copy(items = items)
        }
    }

    override fun getItems(): Map<CatalogueMangaSource, SearchItemResult> {
        return mutableState.value.items
    }
}

@Immutable
data class GlobalSearchState(
    val searchQuery: String? = null,
    val items: Map<CatalogueMangaSource, SearchItemResult> = emptyMap(),
) {

    val progress: Int = items.count { it.value !is SearchItemResult.Loading }

    val total: Int = items.size
}
