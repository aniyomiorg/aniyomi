package eu.kanade.presentation.browse.anime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import eu.kanade.presentation.browse.GlobalSearchEmptyResultItem
import eu.kanade.presentation.browse.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.GlobalSearchResultItem
import eu.kanade.presentation.browse.GlobalSearchToolbar
import eu.kanade.presentation.browse.anime.components.GlobalAnimeSearchCardRow
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.ui.browse.anime.migration.search.MigrateAnimeSearchState
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.AnimeSearchItemResult
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun MigrateAnimeSearchScreen(
    navigateUp: () -> Unit,
    state: MigrateAnimeSearchState,
    getAnime: @Composable (Anime) -> State<Anime>,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onClickSource: (AnimeCatalogueSource) -> Unit,
    onClickItem: (Anime) -> Unit,
    onLongClickItem: (Anime) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        MigrateAnimeSearchContent(
            sourceId = state.anime?.source ?: -1,
            items = state.items,
            contentPadding = paddingValues,
            getAnime = getAnime,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}

@Composable
fun MigrateAnimeSearchContent(
    sourceId: Long,
    items: Map<AnimeCatalogueSource, AnimeSearchItemResult>,
    contentPadding: PaddingValues,
    getAnime: @Composable (Anime) -> State<Anime>,
    onClickSource: (AnimeCatalogueSource) -> Unit,
    onClickItem: (Anime) -> Unit,
    onLongClickItem: (Anime) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items.forEach { (source, result) ->
            item(key = source.id) {
                GlobalSearchResultItem(
                    title = if (source.id == sourceId) "▶ ${source.name}" else source.name,
                    subtitle = LocaleHelper.getDisplayName(source.lang),
                    onClick = { onClickSource(source) },
                ) {
                    when (result) {
                        AnimeSearchItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is AnimeSearchItemResult.Success -> {
                            if (result.isEmpty) {
                                GlobalSearchEmptyResultItem()
                                return@GlobalSearchResultItem
                            }

                            GlobalAnimeSearchCardRow(
                                titles = result.result,
                                getAnime = getAnime,
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                            )
                        }
                        is AnimeSearchItemResult.Error -> {
                            GlobalSearchErrorResultItem(message = result.throwable.message)
                        }
                    }
                }
            }
        }
    }
}
