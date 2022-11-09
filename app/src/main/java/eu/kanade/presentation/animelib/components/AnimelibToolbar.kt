package eu.kanade.presentation.animelib.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.animelib.AnimelibState
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.OverflowMenu
import eu.kanade.presentation.components.Pill
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.theme.active
import eu.kanade.tachiyomi.R

@Composable
fun AnimelibToolbar(
    state: AnimelibState,
    title: AnimelibToolbarTitle,
    incognitoMode: Boolean,
    downloadedOnlyMode: Boolean,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickOpenRandomAnime: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
) = when {
    state.selectionMode -> AnimelibSelectionToolbar(
        state = state,
        incognitoMode = incognitoMode,
        downloadedOnlyMode = downloadedOnlyMode,
        onClickUnselectAll = onClickUnselectAll,
        onClickSelectAll = onClickSelectAll,
        onClickInvertSelection = onClickInvertSelection,
    )
    else -> AnimelibRegularToolbar(
        title = title,
        hasFilters = state.hasActiveFilters,
        incognitoMode = incognitoMode,
        downloadedOnlyMode = downloadedOnlyMode,
        searchQuery = state.searchQuery,
        onChangeSearchQuery = { state.searchQuery = it },
        onClickFilter = onClickFilter,
        onClickRefresh = onClickRefresh,
        onClickOpenRandomAnime = onClickOpenRandomAnime,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun AnimelibRegularToolbar(
    title: AnimelibToolbarTitle,
    hasFilters: Boolean,
    incognitoMode: Boolean,
    downloadedOnlyMode: Boolean,
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickOpenRandomAnime: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
) {
    val pillAlpha = if (isSystemInDarkTheme()) 0.12f else 0.08f
    SearchToolbar(
        titleContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title.text,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, false),
                    overflow = TextOverflow.Ellipsis,
                )
                if (title.numberOfAnime != null) {
                    Pill(
                        text = "${title.numberOfAnime}",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = pillAlpha),
                        fontSize = 14.sp,
                    )
                }
            }
        },
        searchQuery = searchQuery,
        onChangeSearchQuery = onChangeSearchQuery,
        actions = {
            val filterTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current
            IconButton(onClick = onClickFilter) {
                Icon(Icons.Outlined.FilterList, contentDescription = stringResource(R.string.action_filter), tint = filterTint)
            }

            OverflowMenu { closeMenu ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.pref_category_library_update)) },
                    onClick = {
                        onClickRefresh()
                        closeMenu()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.action_open_random_manga)) },
                    onClick = {
                        onClickOpenRandomAnime()
                        closeMenu()
                    },
                )
            }
        },
        incognitoMode = incognitoMode,
        downloadedOnlyMode = downloadedOnlyMode,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun AnimelibSelectionToolbar(
    state: AnimelibState,
    incognitoMode: Boolean,
    downloadedOnlyMode: Boolean,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
) {
    AppBar(
        titleContent = { Text(text = "${state.selection.size}") },
        actions = {
            IconButton(onClick = onClickSelectAll) {
                Icon(Icons.Outlined.SelectAll, contentDescription = stringResource(R.string.action_select_all))
            }
            IconButton(onClick = onClickInvertSelection) {
                Icon(Icons.Outlined.FlipToBack, contentDescription = stringResource(R.string.action_select_inverse))
            }
        },
        isActionMode = true,
        onCancelActionMode = onClickUnselectAll,
        incognitoMode = incognitoMode,
        downloadedOnlyMode = downloadedOnlyMode,
    )
}

data class AnimelibToolbarTitle(
    val text: String,
    val numberOfAnime: Int? = null,
)
