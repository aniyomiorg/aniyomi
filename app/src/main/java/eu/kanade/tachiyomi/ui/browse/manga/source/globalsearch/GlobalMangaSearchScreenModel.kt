package eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch

import androidx.compose.runtime.Immutable
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.flow.update
import tachiyomi.domain.source.manga.service.MangaSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class GlobalMangaSearchScreenModel(
    initialQuery: String = "",
    initialExtensionFilter: String = "",
    preferences: BasePreferences = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: MangaSourceManager = Injekt.get(),
) : MangaSearchScreenModel<GlobalMangaSearchState>(
    GlobalMangaSearchState(
        searchQuery = initialQuery,
    ),
) {

    val incognitoMode = preferences.incognitoMode()
    val lastUsedSourceId = sourcePreferences.lastUsedMangaSource()

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || initialExtensionFilter.isNotBlank()) {
            search(initialQuery)
        }
    }

    override fun getEnabledSources(): List<CatalogueSource> {
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

    override fun updateItems(items: Map<CatalogueSource, MangaSearchItemResult>) {
        mutableState.update {
            it.copy(items = items)
        }
    }

    override fun getItems(): Map<CatalogueSource, MangaSearchItemResult> {
        return mutableState.value.items
    }
}

@Immutable
data class GlobalMangaSearchState(
    val searchQuery: String? = null,
    val items: Map<CatalogueSource, MangaSearchItemResult> = emptyMap(),
) {

    val progress: Int = items.count { it.value !is MangaSearchItemResult.Loading }

    val total: Int = items.size
}
