package eu.kanade.presentation.browse.manga

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import eu.kanade.presentation.browse.GlobalSearchEmptyResultItem
import eu.kanade.presentation.browse.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.GlobalSearchResultItem
import eu.kanade.presentation.browse.GlobalSearchToolbar
import eu.kanade.presentation.browse.manga.components.GlobalMangaSearchCardRow
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.manga.migration.search.MigrateMangaSearchState
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.MangaSearchItemResult
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun MigrateMangaSearchScreen(
    navigateUp: () -> Unit,
    state: MigrateMangaSearchState,
    getManga: @Composable (Manga) -> State<Manga>,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
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
        MigrateMangaSearchContent(
            sourceId = state.manga?.source ?: -1,
            items = state.items,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}

@Composable
fun MigrateMangaSearchContent(
    sourceId: Long,
    items: Map<CatalogueSource, MangaSearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
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
                        MangaSearchItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is MangaSearchItemResult.Success -> {
                            if (result.isEmpty) {
                                GlobalSearchEmptyResultItem()
                                return@GlobalSearchResultItem
                            }

                            GlobalMangaSearchCardRow(
                                titles = result.result,
                                getManga = getManga,
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                            )
                        }
                        is MangaSearchItemResult.Error -> {
                            GlobalSearchErrorResultItem(message = result.throwable.message)
                        }
                    }
                }
            }
        }
    }
}
