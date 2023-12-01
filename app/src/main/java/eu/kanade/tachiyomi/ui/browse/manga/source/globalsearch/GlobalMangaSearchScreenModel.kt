package eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch

import eu.kanade.tachiyomi.source.CatalogueSource

class GlobalMangaSearchScreenModel(
    initialQuery: String = "",
    initialExtensionFilter: String? = null,
) : MangaSearchScreenModel(
    State(
        searchQuery = initialQuery,
    ),
) {

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || !initialExtensionFilter.isNullOrBlank()) {
            if (extensionFilter != null) {
                // we're going to use custom extension filter instead
                setSourceFilter(MangaSourceFilter.All)
            }
            search()
        }
    }

    override fun getEnabledSources(): List<CatalogueSource> {
        return super.getEnabledSources()
            .filter { state.value.sourceFilter != MangaSourceFilter.PinnedOnly || "${it.id}" in pinnedSources }
    }
}
