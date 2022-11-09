package eu.kanade.presentation.animelib

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.util.fastAll
import eu.kanade.domain.anime.model.isLocal
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.library.model.display
import eu.kanade.domain.manga.model.isLocal
import eu.kanade.presentation.animelib.components.AnimelibContent
import eu.kanade.presentation.animelib.components.AnimelibToolbar
import eu.kanade.presentation.components.AnimelibBottomActionMenu
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.EmptyScreenAction
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.animelib.AnimelibPresenter
import eu.kanade.tachiyomi.widget.TachiyomiBottomNavigationView

@Composable
fun AnimelibScreen(
    presenter: AnimelibPresenter,
    onAnimeClicked: (Long) -> Unit,
    onContinueWatchingClicked: (AnimelibAnime) -> Unit,
    onGlobalSearchClicked: () -> Unit,
    onChangeCategoryClicked: () -> Unit,
    onMarkAsSeenClicked: () -> Unit,
    onMarkAsUnseenClicked: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    onDeleteClicked: () -> Unit,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: (Category?) -> Boolean,
    onClickOpenRandomAnime: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = { scrollBehavior ->
            val title by presenter.getToolbarTitle()
            val tabVisible = presenter.tabVisibility && presenter.categories.size > 1
            AnimelibToolbar(
                state = presenter,
                title = title,
                incognitoMode = !tabVisible && presenter.isIncognitoMode,
                downloadedOnlyMode = !tabVisible && presenter.isDownloadOnly,
                onClickUnselectAll = onClickUnselectAll,
                onClickSelectAll = onClickSelectAll,
                onClickInvertSelection = onClickInvertSelection,
                onClickFilter = onClickFilter,
                onClickRefresh = { onClickRefresh(null) },
                onClickOpenRandomAnime = onClickOpenRandomAnime,
                scrollBehavior = scrollBehavior.takeIf { !tabVisible }, // For scroll overlay when no tab
            )
        },
        bottomBar = {
            AnimelibBottomActionMenu(
                visible = presenter.selectionMode,
                onChangeCategoryClicked = onChangeCategoryClicked,
                onMarkAsSeenClicked = onMarkAsSeenClicked,
                onMarkAsUnseenClicked = onMarkAsUnseenClicked,
                onDownloadClicked = onDownloadClicked.takeIf { presenter.selection.fastAll { !it.anime.isLocal() } },
                onDeleteClicked = onDeleteClicked,
            )
        },
    ) { paddingValues ->
        if (presenter.isLoading) {
            LoadingScreen()
            return@Scaffold
        }

        val contentPadding = TachiyomiBottomNavigationView.withBottomNavPadding(paddingValues)
        if (presenter.searchQuery.isNullOrEmpty() && presenter.isLibraryEmpty) {
            val handler = LocalUriHandler.current
            EmptyScreen(
                textResource = R.string.information_empty_library,
                modifier = Modifier.padding(contentPadding),
                actions = listOf(
                    EmptyScreenAction(
                        stringResId = R.string.getting_started_guide,
                        icon = Icons.Outlined.HelpOutline,
                        onClick = { handler.openUri("https://tachiyomi.org/help/guides/getting-started") },
                    ),
                ),
            )
            return@Scaffold
        }

        AnimelibContent(
            state = presenter,
            contentPadding = contentPadding,
            currentPage = { presenter.activeCategory },
            isAnimelibEmpty = presenter.isLibraryEmpty,
            showPageTabs = presenter.tabVisibility,
            showAnimeCount = presenter.animeCountVisibility,
            onChangeCurrentPage = { presenter.activeCategory = it },
            onAnimeClicked = onAnimeClicked,
            onContinueWatchingClicked = onContinueWatchingClicked,
            onToggleSelection = { presenter.toggleSelection(it) },
            onToggleRangeSelection = {
                presenter.toggleRangeSelection(it)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onRefresh = onClickRefresh,
            onGlobalSearchClicked = onGlobalSearchClicked,
            getNumberOfAnimeForCategory = { presenter.getAnimeCountForCategory(it) },
            getDisplayModeForPage = { presenter.categories[it].display },
            getColumnsForOrientation = { presenter.getColumnsPreferenceForCurrentOrientation(it) },
            getAnimelibForPage = { presenter.getAnimeForCategory(page = it) },
            showDownloadBadges = presenter.showDownloadBadges,
            showUnseenBadges = presenter.showUnseenBadges,
            showLocalBadges = presenter.showLocalBadges,
            showLanguageBadges = presenter.showLanguageBadges,
            showContinueWatchingButton = presenter.showContinueWatchingButton,
            isIncognitoMode = presenter.isIncognitoMode,
            isDownloadOnly = presenter.isDownloadOnly,
        )
    }
}
