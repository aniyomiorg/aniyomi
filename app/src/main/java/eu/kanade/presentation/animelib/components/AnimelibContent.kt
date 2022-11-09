package eu.kanade.presentation.animelib.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import eu.kanade.core.prefs.PreferenceMutableState
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.library.model.LibraryDisplayMode
import eu.kanade.presentation.animelib.AnimelibState
import eu.kanade.presentation.components.SwipeRefresh
import eu.kanade.presentation.components.rememberPagerState
import eu.kanade.presentation.library.components.LibraryTabs
import eu.kanade.tachiyomi.ui.animelib.AnimelibItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun AnimelibContent(
    state: AnimelibState,
    contentPadding: PaddingValues,
    currentPage: () -> Int,
    isAnimelibEmpty: Boolean,
    showPageTabs: Boolean,
    showAnimeCount: Boolean,
    onChangeCurrentPage: (Int) -> Unit,
    onAnimeClicked: (Long) -> Unit,
    onContinueWatchingClicked: (AnimelibAnime) -> Unit,
    onToggleSelection: (AnimelibAnime) -> Unit,
    onToggleRangeSelection: (AnimelibAnime) -> Unit,
    onRefresh: (Category?) -> Boolean,
    onGlobalSearchClicked: () -> Unit,
    getNumberOfAnimeForCategory: @Composable (Long) -> State<Int?>,
    getDisplayModeForPage: @Composable (Int) -> LibraryDisplayMode,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getAnimelibForPage: @Composable (Int) -> List<AnimelibItem>,
    showDownloadBadges: Boolean,
    showUnseenBadges: Boolean,
    showLocalBadges: Boolean,
    showLanguageBadges: Boolean,
    showContinueWatchingButton: Boolean,
    isDownloadOnly: Boolean,
    isIncognitoMode: Boolean,
) {
    Column(
        modifier = Modifier.padding(
            top = contentPadding.calculateTopPadding(),
            start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
        ),
    ) {
        val categories = state.categories
        val coercedCurrentPage = remember { currentPage().coerceAtMost(categories.lastIndex) }
        val pagerState = rememberPagerState(coercedCurrentPage)

        val scope = rememberCoroutineScope()
        var isRefreshing by remember(pagerState.currentPage) { mutableStateOf(false) }

        if (isAnimelibEmpty.not() && showPageTabs && categories.size > 1) {
            LibraryTabs(
                categories = categories,
                currentPageIndex = pagerState.currentPage,
                showMangaCount = showAnimeCount,
                getNumberOfMangaForCategory = getNumberOfAnimeForCategory,
                isDownloadOnly = isDownloadOnly,
                isIncognitoMode = isIncognitoMode,
                onTabItemClick = { scope.launch { pagerState.animateScrollToPage(it) } },
            )
        }

        val onClickAnime = { anime: AnimelibAnime ->
            if (state.selectionMode.not()) {
                onAnimeClicked(anime.anime.id)
            } else {
                onToggleSelection(anime)
            }
        }
        val onLongClickAnime = { anime: AnimelibAnime ->
            onToggleRangeSelection(anime)
        }
        val onClickContinueWatching = { anime: AnimelibAnime ->
            onContinueWatchingClicked(anime)
        }

        SwipeRefresh(
            refreshing = isRefreshing,
            onRefresh = {
                val started = onRefresh(categories[currentPage()])
                if (!started) return@SwipeRefresh
                scope.launch {
                    // Fake refresh status but hide it after a second as it's a long running task
                    isRefreshing = true
                    delay(1.seconds)
                    isRefreshing = false
                }
            },
            enabled = state.selectionMode.not(),
        ) {
            AnimelibPager(
                state = pagerState,
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                pageCount = categories.size,
                selectedAnime = state.selection,
                getDisplayModeForPage = getDisplayModeForPage,
                getColumnsForOrientation = getColumnsForOrientation,
                getAnimelibForPage = getAnimelibForPage,
                showDownloadBadges = showDownloadBadges,
                showUnreadBadges = showUnseenBadges,
                showLocalBadges = showLocalBadges,
                showLanguageBadges = showLanguageBadges,
                showContinueWatchingButton = showContinueWatchingButton,
                onClickAnime = onClickAnime,
                onLongClickAnime = onLongClickAnime,
                onClickContinueWatching = onClickContinueWatching,
                onGlobalSearchClicked = onGlobalSearchClicked,
                searchQuery = state.searchQuery,
            )
        }

        LaunchedEffect(pagerState.currentPage) {
            onChangeCurrentPage(pagerState.currentPage)
        }
    }
}
