package eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch

import androidx.compose.runtime.Immutable
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
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
) : AnimeSearchScreenModel<GlobalAnimeSearchState>(
    GlobalAnimeSearchState(
        searchQuery = initialQuery,
    ),
) {

    val incognitoMode = preferences.incognitoMode()
    val lastUsedSourceId = sourcePreferences.lastUsedAnimeSource()

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
}

@Immutable
data class GlobalAnimeSearchState(
    val searchQuery: String? = null,
    val items: Map<AnimeCatalogueSource, AnimeSearchItemResult> = emptyMap(),
) {

    val progress: Int = items.count { it.value !is AnimeSearchItemResult.Loading }

    val total: Int = items.size
}
