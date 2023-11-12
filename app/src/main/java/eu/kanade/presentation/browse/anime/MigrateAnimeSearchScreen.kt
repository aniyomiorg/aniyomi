package eu.kanade.presentation.browse.anime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import eu.kanade.presentation.browse.anime.components.GlobalAnimeSearchToolbar
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.ui.browse.anime.migration.search.MigrateAnimeSearchScreenModel
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.AnimeSourceFilter
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun MigrateAnimeSearchScreen(
    state: MigrateAnimeSearchScreenModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (AnimeSourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getAnime: @Composable (Anime) -> State<Anime>,
    onClickSource: (AnimeCatalogueSource) -> Unit,
    onClickItem: (Anime) -> Unit,
    onLongClickItem: (Anime) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalAnimeSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                sourceFilter = state.sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter,
                onlyShowHasResults = state.onlyShowHasResults,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        GlobalSearchContent(
            fromSourceId = state.anime?.source ?: -1,
            items = state.filteredItems,
            contentPadding = paddingValues,
            getAnime = getAnime,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}
