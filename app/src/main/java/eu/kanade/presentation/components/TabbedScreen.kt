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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.widget.TachiyomiBottomNavigationView
import kotlinx.coroutines.launch

@Composable
fun TabbedScreen(
    @StringRes titleRes: Int?,
    tabs: List<TabContent>,
    startIndex: Int? = null,
    searchQuery: String? = null,
    onChangeSearchQuery: (String?) -> Unit = {},
    incognitoMode: Boolean = false,
    downloadedOnlyMode: Boolean = false,
    state: PagerState = rememberPagerState(),
    scrollable: Boolean = false,
    searchQueryAnime: String? = null,
    onChangeSearchQueryAnime: (String?) -> Unit = {},
) {
    val scope = rememberCoroutineScope()

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
                    1 -> searchQuery // History and Browse
                    else -> searchQueryAnime
                }

                val actualOnChange = when (state.currentPage % 2) {
                    1 -> onChangeSearchQuery // History and Browse
                    else -> onChangeSearchQueryAnime
                }

                val appBarTitleText = if (tab.numberTitle == 0) stringResource(titleRes) else tab.numberTitle.toString()
                SearchToolbar(
                    titleContent = { AppBarTitle(appBarTitleText) },
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
                indicator = { TabIndicator(it[state.currentPage]) },
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

            AppStateBanners(downloadedOnlyMode, incognitoMode)

            HorizontalPager(
                count = tabs.size,
                modifier = Modifier.fillMaxSize(),
                state = state,
                verticalAlignment = Alignment.Top,
            ) { page ->
                tabs[page].content(
                    TachiyomiBottomNavigationView.withBottomNavPadding(
                        PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                    ),
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
    val content: @Composable (contentPadding: PaddingValues) -> Unit,
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
