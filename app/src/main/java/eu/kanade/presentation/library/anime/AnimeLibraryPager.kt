package eu.kanade.presentation.library.anime

import android.content.res.Configuration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.presentation.library.components.GlobalSearchItem
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryItem
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun AnimeLibraryPager(
    state: PagerState,
    contentPadding: PaddingValues,
    hasActiveFilters: Boolean,
    selectedAnime: List<LibraryAnime>,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getLibraryForPage: (Int) -> List<AnimeLibraryItem>,
    onClickAnime: (LibraryAnime) -> Unit,
    onLongClickAnime: (LibraryAnime) -> Unit,
    onClickContinueWatching: ((LibraryAnime) -> Unit)?,
) {
    BoxWithConstraints {
        val density = LocalDensity.current
        val containerHeightPx = with(density) { this@BoxWithConstraints.maxHeight.roundToPx() }

        HorizontalPager(
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

            val displayMode by getDisplayMode(page)
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val columns by remember(isLandscape) { getColumnsForOrientation(isLandscape) }

            when (displayMode) {
                LibraryDisplayMode.List -> {
                    AnimeLibraryList(
                        items = library,
                        entries = columns,
                        containerHeight = containerHeightPx,
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
}

@Composable
private fun LibraryPagerEmptyScreen(
    searchQuery: String?,
    hasActiveFilters: Boolean,
    contentPadding: PaddingValues,
    onGlobalSearchClicked: () -> Unit,
) {
    val msg = when {
        !searchQuery.isNullOrEmpty() -> MR.strings.no_results_found
        hasActiveFilters -> MR.strings.error_no_match
        else -> MR.strings.information_no_manga_category
    }

    Column(
        modifier = Modifier
            .padding(contentPadding + PaddingValues(8.dp))
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (!searchQuery.isNullOrEmpty()) {
            GlobalSearchItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                searchQuery = searchQuery,
                onClick = onGlobalSearchClicked,
            )
        }

        EmptyScreen(
            stringRes = msg,
            modifier = Modifier.weight(1f),
        )
    }
}
