package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.FeedOrderListItem
import eu.kanade.tachiyomi.ui.browse.feed.FeedScreenState
import tachiyomi.domain.source.anime.model.FeedSavedSearch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun FeedOrderScreen(
    state: FeedScreenState,
    onClickDelete: (FeedSavedSearch) -> Unit,
    onClickMoveUp: (FeedSavedSearch) -> Unit,
    onClickMoveDown: (FeedSavedSearch) -> Unit,
) {
    when {
        state.isLoading -> LoadingScreen()
        state.isEmpty -> EmptyScreen(
            stringRes = MR.strings.empty_screen,
        )

        else -> {
            val lazyListState = rememberLazyListState()
            val feeds = state.items ?: emptyList()
            LazyColumn(
                state = lazyListState,
                contentPadding = topSmallPaddingValues +
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
