package eu.kanade.tachiyomi.ui.browse.anime.migration.search

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.AnimeSearchItemResult
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.AnimeSearchScreenModel
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.AnimeSourceFilter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.model.Anime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateAnimeSearchScreenModel(
    val animeId: Long,
    initialExtensionFilter: String = "",
    getAnime: GetAnime = Injekt.get(),
) : AnimeSearchScreenModel<MigrateAnimeSearchScreenModel.State>(State()) {

    init {
        extensionFilter = initialExtensionFilter
        coroutineScope.launch {
            val anime = getAnime.await(animeId)!!

            mutableState.update {
                it.copy(anime = anime, searchQuery = anime.title)
            }

            search(anime.title)
        }
    }

    override fun getEnabledSources(): List<AnimeCatalogueSource> {
        return super.getEnabledSources()
            .filter { mutableState.value.sourceFilter != AnimeSourceFilter.PinnedOnly || "${it.id}" in pinnedSources }
            .sortedWith(
                compareBy(
                    { it.id != state.value.anime!!.source },
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

    fun setDialog(dialog: Dialog?) {
        mutableState.update {
            it.copy(dialog = dialog)
        }
    }

    @Immutable
    data class State(
        val anime: Anime? = null,
        val dialog: Dialog? = null,

        val searchQuery: String? = null,
        val sourceFilter: AnimeSourceFilter = AnimeSourceFilter.PinnedOnly,
        val onlyShowHasResults: Boolean = false,
        val items: Map<AnimeCatalogueSource, AnimeSearchItemResult> = emptyMap(),
    ) {
        val progress: Int = items.count { it.value !is AnimeSearchItemResult.Loading }
        val total: Int = items.size
        val filteredItems = items.filter { (_, result) -> result.isVisible(onlyShowHasResults) }
    }

    sealed class Dialog {
        data class Migrate(val anime: Anime) : Dialog()
    }
}
