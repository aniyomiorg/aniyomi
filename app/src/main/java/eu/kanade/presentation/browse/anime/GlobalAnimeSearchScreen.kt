package eu.kanade.presentation.browse.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.browse.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.GlobalSearchResultItem
import eu.kanade.presentation.browse.GlobalSearchToolbar
import eu.kanade.presentation.browse.anime.components.GlobalAnimeSearchCardRow
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.AnimeSearchItemResult
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.AnimeSourceFilter
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreenModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding

@Composable
fun GlobalAnimeSearchScreen(
    state: GlobalAnimeSearchScreenModel.State,
    items: Map<AnimeCatalogueSource, AnimeSearchItemResult>,
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
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                GlobalSearchToolbar(
                    searchQuery = state.searchQuery,
                    progress = state.progress,
                    total = state.total,
                    navigateUp = navigateUp,
                    onChangeSearchQuery = onChangeSearchQuery,
                    onSearch = onSearch,
                    scrollBehavior = scrollBehavior,
                )

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = MaterialTheme.padding.small),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    // TODO: make this UX better; it only applies when triggering a new search
                    FilterChip(
                        selected = state.sourceFilter == AnimeSourceFilter.PinnedOnly,
                        onClick = { onChangeSearchFilter(AnimeSourceFilter.PinnedOnly) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(FilterChipDefaults.IconSize),
                            )
                        },
                        label = {
                            Text(text = stringResource(id = R.string.pinned_sources))
                        },
                    )
                    FilterChip(
                        selected = state.sourceFilter == AnimeSourceFilter.All,
                        onClick = { onChangeSearchFilter(AnimeSourceFilter.All) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.DoneAll,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(FilterChipDefaults.IconSize),
                            )
                        },
                        label = {
                            Text(text = stringResource(id = R.string.all))
                        },
                    )

                    FilterChip(
                        selected = state.onlyShowHasResults,
                        onClick = { onToggleResults() },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(FilterChipDefaults.IconSize),
                            )
                        },
                        label = {
                            Text(text = stringResource(id = R.string.has_results))
                        },
                    )
                }

                Divider()
            }
        },
    ) { paddingValues ->
        GlobalAnimeSearchContent(
            items = items,
            contentPadding = paddingValues,
            getAnime = getAnime,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}

@Composable
private fun GlobalAnimeSearchContent(
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
                    title = source.name,
                    subtitle = LocaleHelper.getDisplayName(source.lang),
                    onClick = { onClickSource(source) },
                ) {
                    when (result) {
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
