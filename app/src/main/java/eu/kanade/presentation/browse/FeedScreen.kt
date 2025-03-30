package eu.kanade.presentation.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.anime.components.AnimeSourceIcon
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.source.anime.getNameForAnimeInfo
import eu.kanade.tachiyomi.ui.browse.feed.FeedScreenState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.source.anime.model.AnimeSource
import tachiyomi.domain.source.anime.model.FeedSavedSearch
import tachiyomi.domain.source.anime.model.SavedSearch
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import kotlin.time.Duration.Companion.seconds

data class FeedItemUI(
    val feed: FeedSavedSearch,
    val savedSearch: SavedSearch?,
    val source: AnimeCatalogueSource?,
    val title: String,
    val subtitle: String,
    val results: List<Anime>?,
)

@Composable
fun FeedScreen(
    state: FeedScreenState,
    contentPadding: PaddingValues,
    onClickSavedSearch: (SavedSearch, AnimeCatalogueSource) -> Unit,
    onClickSource: (AnimeCatalogueSource) -> Unit,
    // KMK -->
    onLongClickFeed: (FeedItemUI, Boolean, Boolean) -> Unit,
    // KMK <--
    onClickManga: (Anime) -> Unit,
    // KMK -->
    onLongClickManga: (Anime) -> Unit,
    // KMK <--
    onRefresh: () -> Unit,
    getAnimeState: @Composable (Anime) -> State<Anime>,
) {
    when {
        state.isLoading -> LoadingScreen()
        state.isEmpty -> EmptyScreen(
            TLMR.strings.feed_tab_empty,
            modifier = Modifier.padding(contentPadding),
        )
        else -> {
            var refreshing by remember { mutableStateOf(false) }
            LaunchedEffect(refreshing) {
                if (refreshing) {
                    delay(1.seconds)
                    refreshing = false
                }
            }
            PullRefresh(
                refreshing = refreshing && state.isLoadingItems,
                onRefresh = {
                    refreshing = true
                    onRefresh()
                },
                enabled = !state.isLoadingItems,
            ) {
                ScrollbarLazyColumn(
                    contentPadding = contentPadding + topSmallPaddingValues,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // KMK -->
                    val feeds = state.items.orEmpty()
                    itemsIndexed(
                        items = feeds,
                        key = { _, it -> "feed-${it.feed.id}" },
                    ) { index, item ->
                        // KMK <--
                        GlobalSearchResultItem(
                            title = item.title,
                            subtitle = item.subtitle,
                            onClick = {
                                if (item.savedSearch != null && item.source != null) {
                                    onClickSavedSearch(item.savedSearch, item.source)
                                } else if (item.source != null) {
                                    onClickSource(item.source)
                                }
                            },
                            modifier = Modifier.animateItem(),
                        ) {
                            FeedItem(
                                item = item,
                                getMangaState = { getAnimeState(it) },
                                onClickManga = onClickManga,
                                // KMK -->
                                onLongClickManga = onLongClickManga,
                                // KMK <--
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeedItem(
    item: FeedItemUI,
    getMangaState: @Composable ((Anime) -> State<Anime>),
    onClickManga: (Anime) -> Unit,
    // KMK -->
    onLongClickManga: (Anime) -> Unit,
    // KMK <--
) {
    when {
        item.results == null -> {
            GlobalSearchLoadingResultItem()
        }
        item.results.isEmpty() -> {
            GlobalSearchErrorResultItem(message = stringResource(MR.strings.no_results_found))
        }
        else -> {
            GlobalSearchCardRow(
                titles = item.results,
                getAnime = getMangaState,
                onClick = onClickManga,
                // KMK -->
                onLongClick = onLongClickManga,
                // KMK <--
            )
        }
    }
}

@Composable
fun FeedAddDialog(
    sources: ImmutableList<AnimeCatalogueSource>,
    onDismiss: () -> Unit,
    onClickAdd: (AnimeCatalogueSource?) -> Unit,
) {
    // KMK -->
    var query by remember { mutableStateOf("") }
    val sourceList = sources
        .filter { source ->
            if (query.isBlank()) return@filter true
            query.split(",").any {
                val input = it.trim()
                if (input.isEmpty()) return@any false
                source.name.contains(input, ignoreCase = true) ||
                    source.id == input.toLongOrNull()
            }
        }
    val composeOptions: List<@Composable () -> Unit> = sourceList
        .map {
            {
                val source = AnimeSource(
                    id = it.id,
                    lang = it.lang,
                    name = it.name,
                    supportsLatest = it.supportsLatest,
                    isStub = false,
                )
                AnimeSourceIcon(source = source)
                Spacer(modifier = Modifier.width(MaterialTheme.padding.extraSmall))
                Text(text = it.getNameForAnimeInfo())
            }
        }
    // KMK <--
    var selected by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        title = {
            Text(text = stringResource(TLMR.strings.feed))
        },
        text = {
            // KMK -->
            RadioSelectorSearchable(
                options = composeOptions,
                queryString = query,
                onChangeSearchQuery = {
                    query = it ?: ""
                },
                // KMK <--
                selected = selected,
            ) {
                selected = it
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onClickAdd(selected?.let { sourceList[it] }) }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}

@Composable
fun FeedAddSearchDialog(
    source: AnimeCatalogueSource,
    savedSearches: ImmutableList<SavedSearch?>,
    onDismiss: () -> Unit,
    onClickAdd: (AnimeCatalogueSource, SavedSearch?) -> Unit,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        title = {
            Text(text = source.name)
        },
        text = {
            val context = LocalContext.current
            val savedSearchStrings = remember {
                savedSearches.map {
                    it?.name
                        // KMK -->
                        ?: if (source.supportsLatest) {
                            // KMK <--
                            context.stringResource(MR.strings.latest)
                            // KMK -->
                        } else {
                            context.stringResource(MR.strings.popular)
                        }
                    // KMK <--
                }.toImmutableList()
            }
            RadioSelectorSearchable(
                options = savedSearches,
                optionStrings = savedSearchStrings,
                selected = selected,
            ) {
                selected = it
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onClickAdd(source, selected?.let { savedSearches[it] }) },
                // KMK -->
                enabled = selected != null,
                // KMK <--
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}

@Composable
fun <T> RadioSelectorSearchable(
    options: ImmutableList<T>,
    optionStrings: ImmutableList<String> = remember { options.map { it.toString() }.toImmutableList() },
    selected: Int?,
    onSelectOption: (Int) -> Unit = {},
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        optionStrings.forEachIndexed { index, option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { onSelectOption(index) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected == index, onClick = null)
                Spacer(Modifier.width(MaterialTheme.padding.extraSmall))
                Text(option, maxLines = 1)
            }
        }
    }
}

// KMK -->
@Composable
fun RadioSelectorSearchable(
    options: List<@Composable () -> Unit>,
    queryString: String? = null,
    onChangeSearchQuery: ((String?) -> Unit)? = null,
    selected: Int?,
    onSelectOption: (Int) -> Unit = {},
) {
    BackHandler(enabled = !queryString.isNullOrBlank()) {
        onChangeSearchQuery?.invoke("")
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        options.forEachIndexed { index, option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { onSelectOption(index) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected == index, onClick = null)
                Spacer(Modifier.width(MaterialTheme.padding.extraSmall))
                option()
            }
        }
    }
}
// KMK <--
