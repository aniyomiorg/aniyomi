package eu.kanade.presentation.animelib.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import eu.kanade.core.prefs.PreferenceMutableState
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.domain.library.model.LibraryDisplayMode
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.HorizontalPager
import eu.kanade.presentation.components.PagerState
import eu.kanade.presentation.library.components.LibraryPagerEmptyScreen
import eu.kanade.presentation.util.plus
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.animelib.AnimelibItem

@Composable
fun AnimelibPager(
    state: PagerState,
    contentPadding: PaddingValues,
    pageCount: Int,
    hasActiveFilters: Boolean,
    selectedAnime: List<AnimelibAnime>,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
    getDisplayModeForPage: @Composable (Int) -> LibraryDisplayMode,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getAnimelibForPage: (Int) -> List<AnimelibItem>,
    onClickAnime: (AnimelibAnime) -> Unit,
    onLongClickAnime: (AnimelibAnime) -> Unit,
    onClickContinueWatching: ((AnimelibAnime) -> Unit)?,
) {
    HorizontalPager(
        count = pageCount,
        modifier = Modifier.fillMaxSize(),
        state = state,
        verticalAlignment = Alignment.Top,
    ) { page ->
        if (page !in ((state.currentPage - 1)..(state.currentPage + 1))) {
            // To make sure only one offscreen page is being composed
            return@HorizontalPager
        }
        val library = getAnimelibForPage(page)

        if (library.isEmpty()) {
            LibraryPagerEmptyScreen(
                searchQuery = searchQuery,
                hasActiveFilters = hasActiveFilters,
                contentPadding = contentPadding,
                onGlobalSearchClicked = onGlobalSearchClicked,
            )
            return@HorizontalPager
        }

        val displayMode = getDisplayModeForPage(page)
        val columns by if (displayMode != LibraryDisplayMode.List) {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            remember(isLandscape) { getColumnsForOrientation(isLandscape) }
        } else {
            remember { mutableStateOf(0) }
        }

        when (displayMode) {
            LibraryDisplayMode.List -> {
                AnimelibList(
                    items = library,
                    contentPadding = contentPadding,
                    selection = selectedAnime,
                    onClick = onClickAnime,
                    onClickContinueWatching = onClickContinueWatching,
                    onLongClick = onLongClickAnime,
                    searchQuery = searchQuery,
                    onGlobalSearchClicked = onGlobalSearchClicked,
                )
            }
            LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
                AnimelibCompactGrid(
                    items = library,
                    showTitle = displayMode is LibraryDisplayMode.CompactGrid,
                    columns = columns,
                    contentPadding = contentPadding,
                    selection = selectedAnime,
                    onClick = onClickAnime,
                    onClickContinueWatching = onClickContinueWatching,
                    onLongClick = onLongClickAnime,
                    searchQuery = searchQuery,
                    onGlobalSearchClicked = onGlobalSearchClicked,
                )
            }
            LibraryDisplayMode.ComfortableGrid -> {
                AnimelibComfortableGrid(
                    items = library,
                    columns = columns,
                    contentPadding = contentPadding,
                    selection = selectedAnime,
                    onClick = onClickAnime,
                    onLongClick = onLongClickAnime,
                    onClickContinueWatching = onClickContinueWatching,
                    searchQuery = searchQuery,
                    onGlobalSearchClicked = onGlobalSearchClicked,
                )
            }
        }
    }
}
