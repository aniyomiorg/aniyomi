package eu.kanade.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.HorizontalPager
import tachiyomi.presentation.core.components.PagerState
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TabIndicator
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.components.rememberPagerState

@Composable
fun TabbedScreen(
    @StringRes titleRes: Int?,
    tabs: List<TabContent>,
    startIndex: Int? = null,
    mangaSearchQuery: String? = null,
    onChangeMangaSearchQuery: (String?) -> Unit = {},
    state: PagerState = rememberPagerState(),
    scrollable: Boolean = false,
    animeSearchQuery: String? = null,
    onChangeAnimeSearchQuery: (String?) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(startIndex) {
        if (startIndex != null) {
            state.scrollToPage(startIndex)
        }
    }

    Scaffold(
        topBar = {
            if (titleRes != null) {
                val tab = tabs[state.currentPage]
                val searchEnabled = tab.searchEnabled

                val actualQuery = when (state.currentPage % 2) {
                    1 -> mangaSearchQuery // History and Browse
                    else -> animeSearchQuery
                }

                val actualOnChange = when (state.currentPage % 2) {
                    1 -> onChangeMangaSearchQuery // History and Browse
                    else -> onChangeAnimeSearchQuery
                }

                SearchToolbar(
                    titleContent = { AppBarTitle(stringResource(titleRes), null, tab.numberTitle) },
                    searchEnabled = searchEnabled,
                    searchQuery = if (searchEnabled) actualQuery else null,
                    onChangeSearchQuery = actualOnChange,
                    actions = { AppBarActions(tab.actions) },
                    cancelAction = tab.cancelAction,
                    actionMode = tab.numberTitle != 0,
                    navigateUp = tab.navigateUp,
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier.padding(
                top = contentPadding.calculateTopPadding(),
                start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
            ),
        ) {
            FlexibleTabRow(
                scrollable = scrollable,
                selectedTabIndex = state.currentPage,
                indicator = { TabIndicator(it[state.currentPage], state.currentPageOffsetFraction) },
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = state.currentPage == index,
                        onClick = { scope.launch { state.animateScrollToPage(index) } },
                        text = { TabText(text = stringResource(tab.titleRes), badgeCount = tab.badgeNumber) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalPager(
                count = tabs.size,
                modifier = Modifier.fillMaxSize(),
                state = state,
                verticalAlignment = Alignment.Top,
            ) { page ->
                tabs[page].content(
                    PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                    snackbarHostState,
                )
            }
        }
    }
}

data class TabContent(
    @StringRes val titleRes: Int,
    val badgeNumber: Int? = null,
    val searchEnabled: Boolean = false,
    val actions: List<AppBar.Action> = emptyList(),
    val content: @Composable (contentPadding: PaddingValues, snackbarHostState: SnackbarHostState) -> Unit,
    val numberTitle: Int = 0,
    val cancelAction: () -> Unit = {},
    val navigateUp: (() -> Unit)? = null,
)

@Composable
private fun FlexibleTabRow(
    scrollable: Boolean,
    selectedTabIndex: Int,
    indicator: @Composable (List<TabPosition>) -> Unit,
    block: @Composable () -> Unit,
) {
    return if (scrollable) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            indicator = indicator,
            edgePadding = 13.dp,
        ) {
            block()
        }
    } else {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            indicator = indicator,
        ) {
            block()
        }
    }
}
