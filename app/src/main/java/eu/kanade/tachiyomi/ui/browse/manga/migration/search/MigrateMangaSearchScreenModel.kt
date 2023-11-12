package eu.kanade.tachiyomi.ui.browse.manga.migration.search

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.MangaSearchItemResult
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.MangaSearchScreenModel
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.MangaSourceFilter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.manga.interactor.GetManga
import tachiyomi.domain.entries.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateSearchScreenModel(
    val mangaId: Long,
    initialExtensionFilter: String = "",
    getManga: GetManga = Injekt.get(),
) : MangaSearchScreenModel<MigrateSearchScreenModel.State>(State()) {

    init {
        extensionFilter = initialExtensionFilter
        coroutineScope.launch {
            val manga = getManga.await(mangaId)!!

            mutableState.update {
                it.copy(manga = manga, searchQuery = manga.title)
            }

            search(manga.title)
        }
    }

    override fun getEnabledSources(): List<CatalogueSource> {
        return super.getEnabledSources()
            .filter { mutableState.value.sourceFilter != MangaSourceFilter.PinnedOnly || "${it.id}" in pinnedSources }
            .sortedWith(
                compareBy(
                    { it.id != state.value.manga!!.source },
                    { "${it.id}" !in pinnedSources },
                    { "${it.name.lowercase()} (${it.lang})" },
                ),
            )
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

    override fun setSourceFilter(filter: MangaSourceFilter) {
        mutableState.update { it.copy(sourceFilter = filter) }
    }

    override fun toggleFilterResults() {
        mutableState.update {
            it.copy(onlyShowHasResults = !it.onlyShowHasResults)
        }
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update {
            it.copy(dialog = dialog)
        }
    }

    @Immutable
    data class State(
        val manga: Manga? = null,
        val dialog: Dialog? = null,

        val searchQuery: String? = null,
        val sourceFilter: MangaSourceFilter = MangaSourceFilter.PinnedOnly,
        val onlyShowHasResults: Boolean = false,
        val items: Map<CatalogueSource, MangaSearchItemResult> = emptyMap(),
    ) {
        val progress: Int = items.count { it.value !is MangaSearchItemResult.Loading }
        val total: Int = items.size
        val filteredItems = items.filter { (_, result) -> result.isVisible(onlyShowHasResults) }
    }

    sealed class Dialog {
        data class Migrate(val manga: Manga) : Dialog()
    }
}
