package eu.kanade.presentation.browse.anime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.presentation.browse.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.GlobalSearchResultItem
import eu.kanade.presentation.browse.GlobalSearchToolbar
import eu.kanade.presentation.browse.anime.components.GlobalAnimeSearchCardRow
import eu.kanade.presentation.components.LazyColumn
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.util.padding
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.AnimeSearchItemResult
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchState
import eu.kanade.tachiyomi.util.system.LocaleHelper

@Composable
fun GlobalAnimeSearchScreen(
    state: GlobalAnimeSearchState,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    getAnime: @Composable (AnimeCatalogueSource, Anime) -> State<Anime>,
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
        GlobalAnimeSearchContent(
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
fun GlobalAnimeSearchContent(
    items: Map<AnimeCatalogueSource, AnimeSearchItemResult>,
    contentPadding: PaddingValues,
    getAnime: @Composable (AnimeCatalogueSource, Anime) -> State<Anime>,
    onClickSource: (AnimeCatalogueSource) -> Unit,
    onClickItem: (Anime) -> Unit,
    onLongClickItem: (Anime) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items.forEach { (source, result) ->
            item {
                GlobalSearchResultItem(
                    title = source.name,
                    subtitle = LocaleHelper.getDisplayName(source.lang),
                    onClick = { onClickSource(source) },
                ) {
                    when (result) {
                        is AnimeSearchItemResult.Error -> {
                            GlobalSearchErrorResultItem(message = result.throwable.message)
                        }
                        AnimeSearchItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is AnimeSearchItemResult.Success -> {
                            if (result.isEmpty) {
                                Text(
                                    text = stringResource(R.string.no_results_found),
                                    modifier = Modifier
                                        .padding(
                                            horizontal = MaterialTheme.padding.medium,
                                            vertical = MaterialTheme.padding.small,
                                        ),
                                )
                                return@GlobalSearchResultItem
                            }

                            GlobalAnimeSearchCardRow(
                                titles = result.result,
                                getAnime = { getAnime(source, it) },
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                            )
                        }
                    }
                }
            }
        }
    }
}
