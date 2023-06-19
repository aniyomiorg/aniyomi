package eu.kanade.presentation.library.anime

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.presentation.library.manga.LibraryPagerEmptyScreen
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryItem
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.presentation.core.components.HorizontalPager
import tachiyomi.presentation.core.components.PagerState

@Composable
fun AnimeLibraryPager(
    state: PagerState,
    contentPadding: PaddingValues,
    pageCount: Int,
    hasActiveFilters: Boolean,
    selectedAnime: List<LibraryAnime>,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
    getDisplayModeForPage: @Composable (Int) -> LibraryDisplayMode,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getLibraryForPage: (Int) -> List<AnimeLibraryItem>,
    onClickAnime: (LibraryAnime) -> Unit,
    onLongClickAnime: (LibraryAnime) -> Unit,
    onClickContinueWatching: ((LibraryAnime) -> Unit)?,
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
        val library = getLibraryForPage(page)

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
                AnimeLibraryList(
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
                AnimeLibraryCompactGrid(
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
                AnimeLibraryComfortableGrid(
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
