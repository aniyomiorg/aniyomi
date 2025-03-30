package eu.kanade.presentation.browse.anime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.FeedOrderListItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.tachiyomi.ui.browse.anime.source.feed.SourceFeedState
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.source.anime.model.FeedSavedSearch
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun SourceFeedOrderScreen(
    state: SourceFeedState,
    onClickDelete: (FeedSavedSearch) -> Unit,
    onClickMoveUp: (FeedSavedSearch) -> Unit,
    onClickMoveDown: (FeedSavedSearch) -> Unit,
    onClickSortAlphabetically: () -> Unit,
    navigateUp: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                titleContent = {
                    AppBarTitle(stringResource(TLMR.strings.action_sort_feed))
                },
                navigateUp = navigateUp,
                actions = {
                    persistentListOf(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_sort),
                            icon = Icons.Outlined.SortByAlpha,
                            onClick = onClickSortAlphabetically,
                        ),
                    )
                },
                isActionMode = false,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingScreen()
            state.items
                .filterIsInstance<SourceFeedUI.SourceSavedSearch>()
                .isEmpty() -> EmptyScreen(
                stringRes = MR.strings.empty_screen,
            )

            else -> {
                val lazyListState = rememberLazyListState()
                val feeds = state.items
                    .filterIsInstance<SourceFeedUI.SourceSavedSearch>()
                LazyColumn(
                    state = lazyListState,
                    contentPadding = paddingValues + topSmallPaddingValues +
                        PaddingValues(horizontal = MaterialTheme.padding.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    itemsIndexed(
                        items = feeds,
                        key = { _, feed -> "feed-${feed.feed.id}" },
                    ) { index, feed ->
                        FeedOrderListItem(
                            modifier = Modifier.animateItem(),
                            title = feed.title,
                            canMoveUp = index != 0,
                            canMoveDown = index != feeds.lastIndex,
                            onMoveUp = { onClickMoveUp(feed.feed) },
                            onMoveDown = { onClickMoveDown(feed.feed) },
                            onDelete = { onClickDelete(feed.feed) },
                        )
                    }
                }
            }
        }
    }
}
