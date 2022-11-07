package eu.kanade.presentation.animebrowse

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourcePresenter
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourcePresenter.AnimeFilter
import eu.kanade.tachiyomi.ui.browse.animesource.browse.toItems

@Stable
interface BrowseAnimeSourceState {
    val source: AnimeCatalogueSource?
    var searchQuery: String?
    val currentFilter: AnimeFilter
    val isUserQuery: Boolean
    val filters: AnimeFilterList
    val filterItems: List<IFlexible<*>>
    var dialog: BrowseAnimeSourcePresenter.Dialog?
}

fun BrowseAnimeSourceState(initialQuery: String?): BrowseAnimeSourceState {
    return when (val filter = AnimeFilter.valueOf(initialQuery ?: "")) {
        AnimeFilter.Latest, AnimeFilter.Popular -> BrowseAnimeSourceStateImpl(initialCurrentFilter = filter)
        is AnimeFilter.UserInput -> BrowseAnimeSourceStateImpl(initialQuery = initialQuery, initialCurrentFilter = filter)
    }
}

class BrowseAnimeSourceStateImpl(initialQuery: String? = null, initialCurrentFilter: AnimeFilter) :
    BrowseAnimeSourceState {
    override var source: AnimeCatalogueSource? by mutableStateOf(null)
    override var searchQuery: String? by mutableStateOf(initialQuery)
    override var currentFilter: AnimeFilter by mutableStateOf(initialCurrentFilter)
    override val isUserQuery: Boolean by derivedStateOf { currentFilter is AnimeFilter.UserInput && currentFilter.query.isNotEmpty() }
    override var filters: AnimeFilterList by mutableStateOf(AnimeFilterList())
    override val filterItems: List<IFlexible<*>> by derivedStateOf { filters.toItems() }
    override var dialog: BrowseAnimeSourcePresenter.Dialog? by mutableStateOf(null)
}
