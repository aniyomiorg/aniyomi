package eu.kanade.presentation.library.anime

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.presentation.library.LibraryTabs
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.rememberPagerState
import kotlin.time.Duration.Companion.seconds

@Composable
fun AnimeLibraryContent(
    categories: List<Category>,
    searchQuery: String?,
    selection: List<LibraryAnime>,
    contentPadding: PaddingValues,
    currentPage: () -> Int,
    hasActiveFilters: Boolean,
    showPageTabs: Boolean,
    onChangeCurrentPage: (Int) -> Unit,
    onAnimeClicked: (Long) -> Unit,
    onContinueWatchingClicked: ((LibraryAnime) -> Unit)?,
    onToggleSelection: (LibraryAnime) -> Unit,
    onToggleRangeSelection: (LibraryAnime) -> Unit,
    onRefresh: (Category?) -> Boolean,
    onGlobalSearchClicked: () -> Unit,
    getNumberOfAnimeForCategory: (Category) -> Int?,
    getDisplayModeForPage: @Composable (Int) -> LibraryDisplayMode,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getAnimeLibraryForPage: (Int) -> List<AnimeLibraryItem>,
) {
    Column(
        modifier = Modifier.padding(
            top = contentPadding.calculateTopPadding(),
            start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
        ),
    ) {
        val coercedCurrentPage = remember { currentPage().coerceAtMost(categories.lastIndex) }
        val pagerState = rememberPagerState(coercedCurrentPage)

        val scope = rememberCoroutineScope()
        var isRefreshing by remember(pagerState.currentPage) { mutableStateOf(false) }

        if (showPageTabs && categories.size > 1) {
            if (categories.size <= pagerState.currentPage) {
                pagerState.currentPage = categories.size - 1
            }
            LibraryTabs(
                categories = categories,
                pagerState = pagerState,
                getNumberOfItemsForCategory = getNumberOfAnimeForCategory,
            ) { scope.launch { pagerState.animateScrollToPage(it) } }
        }

        val notSelectionMode = selection.isEmpty()
        val onClickAnime = { anime: LibraryAnime ->
            if (notSelectionMode) {
                onAnimeClicked(anime.anime.id)
            } else {
                onToggleSelection(anime)
            }
        }

        PullRefresh(
            refreshing = isRefreshing,
            onRefresh = {
                val started = onRefresh(categories[currentPage()])
                if (!started) return@PullRefresh
                scope.launch {
                    // Fake refresh status but hide it after a second as it's a long running task
                    isRefreshing = true
                    delay(1.seconds)
                    isRefreshing = false
                }
            },
            enabled = notSelectionMode,
        ) {
            AnimeLibraryPager(
                state = pagerState,
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                pageCount = categories.size,
                hasActiveFilters = hasActiveFilters,
                selectedAnime = selection,
                searchQuery = searchQuery,
                onGlobalSearchClicked = onGlobalSearchClicked,
                getDisplayModeForPage = getDisplayModeForPage,
                getColumnsForOrientation = getColumnsForOrientation,
                getLibraryForPage = getAnimeLibraryForPage,
                onClickAnime = onClickAnime,
                onLongClickAnime = onToggleRangeSelection,
                onClickContinueWatching = onContinueWatchingClicked,
            )
        }

        LaunchedEffect(pagerState.currentPage) {
            onChangeCurrentPage(pagerState.currentPage)
        }
    }
}
