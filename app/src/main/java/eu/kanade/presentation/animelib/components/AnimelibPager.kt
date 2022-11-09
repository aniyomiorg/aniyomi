package eu.kanade.presentation.animelib.components

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
import eu.kanade.core.prefs.PreferenceMutableState
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.domain.library.model.LibraryDisplayMode
import eu.kanade.presentation.components.HorizontalPager
import eu.kanade.presentation.components.PagerState
import eu.kanade.tachiyomi.ui.animelib.AnimelibItem

@Composable
fun AnimelibPager(
    state: PagerState,
    contentPadding: PaddingValues,
    pageCount: Int,
    selectedAnime: List<AnimelibAnime>,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
    getDisplayModeForPage: @Composable (Int) -> LibraryDisplayMode,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getAnimelibForPage: @Composable (Int) -> List<AnimelibItem>,
    showDownloadBadges: Boolean,
    showUnreadBadges: Boolean,
    showLocalBadges: Boolean,
    showLanguageBadges: Boolean,
    onClickContinueWatching: (AnimelibAnime) -> Unit,
    onClickAnime: (AnimelibAnime) -> Unit,
    onLongClickAnime: (AnimelibAnime) -> Unit,
    showContinueWatchingButton: Boolean,
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
                    showDownloadBadges = showDownloadBadges,
                    showUnreadBadges = showUnreadBadges,
                    showLocalBadges = showLocalBadges,
                    showLanguageBadges = showLanguageBadges,
                    showContinueWatchingButton = showContinueWatchingButton,
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
                    showDownloadBadges = showDownloadBadges,
                    showUnreadBadges = showUnreadBadges,
                    showLocalBadges = showLocalBadges,
                    showLanguageBadges = showLanguageBadges,
                    showContinueWatchingButton = showContinueWatchingButton,
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
                    showDownloadBadges = showDownloadBadges,
                    showUnreadBadges = showUnreadBadges,
                    showLocalBadges = showLocalBadges,
                    showLanguageBadges = showLanguageBadges,
                    showContinueWatchingButton = showContinueWatchingButton,
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
        }
    }
}
