package eu.kanade.presentation.entries.anime

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import eu.kanade.domain.items.episode.model.Episode
import eu.kanade.presentation.components.EntryBottomActionMenu
import eu.kanade.presentation.components.EpisodeDownloadAction
import eu.kanade.presentation.components.ExtendedFloatingActionButton
import eu.kanade.presentation.components.LazyColumn
import eu.kanade.presentation.components.PullRefresh
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.components.TwoPanelBox
import eu.kanade.presentation.components.VerticalFastScroller
import eu.kanade.presentation.entries.DownloadAction
import eu.kanade.presentation.entries.EntryScreenItem
import eu.kanade.presentation.entries.EntryToolbar
import eu.kanade.presentation.entries.ItemHeader
import eu.kanade.presentation.entries.anime.components.AnimeActionRow
import eu.kanade.presentation.entries.anime.components.AnimeEpisodeListItem
import eu.kanade.presentation.entries.anime.components.AnimeInfoBox
import eu.kanade.presentation.entries.anime.components.ExpandableAnimeDescription
import eu.kanade.presentation.util.isScrolledToEnd
import eu.kanade.presentation.util.isScrollingUp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import eu.kanade.tachiyomi.source.anime.getNameForAnimeInfo
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreenState
import eu.kanade.tachiyomi.ui.entries.anime.EpisodeItem
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun AnimeScreen(
    state: AnimeScreenState.Success,
    snackbarHostState: SnackbarHostState,
    isTabletUi: Boolean,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (episode: Episode, alt: Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,
    onTagClicked: (String) -> Unit,
    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueWatching: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    changeAnimeSkipIntro: (() -> Unit)?,

    // For bottom action menu
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,

    // Episode selection
    onEpisodeSelected: (EpisodeItem, Boolean, Boolean, Boolean) -> Unit,
    onAllEpisodeSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
) {
    if (!isTabletUi) {
        AnimeScreenSmallImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            onBackClicked = onBackClicked,
            onEpisodeClicked = onEpisodeClicked,
            onDownloadEpisode = onDownloadEpisode,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onTagClicked = onTagClicked,
            onFilterClicked = onFilterButtonClicked,
            onRefresh = onRefresh,
            onContinueWatching = onContinueWatching,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            onEditCategoryClicked = onEditCategoryClicked,
            onMigrateClicked = onMigrateClicked,
            changeAnimeSkipIntro = changeAnimeSkipIntro,
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
            onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onEpisodeSelected = onEpisodeSelected,
            onAllEpisodeSelected = onAllEpisodeSelected,
            onInvertSelection = onInvertSelection,
        )
    } else {
        AnimeScreenLargeImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            onBackClicked = onBackClicked,
            onEpisodeClicked = onEpisodeClicked,
            onDownloadEpisode = onDownloadEpisode,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onTagClicked = onTagClicked,
            onFilterButtonClicked = onFilterButtonClicked,
            onRefresh = onRefresh,
            onContinueWatching = onContinueWatching,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            onEditCategoryClicked = onEditCategoryClicked,
            changeAnimeSkipIntro = changeAnimeSkipIntro,
            onMigrateClicked = onMigrateClicked,
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
            onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onEpisodeSelected = onEpisodeSelected,
            onAllEpisodeSelected = onAllEpisodeSelected,
            onInvertSelection = onInvertSelection,
        )
    }
}

@Composable
private fun AnimeScreenSmallImpl(
    state: AnimeScreenState.Success,
    snackbarHostState: SnackbarHostState,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,
    onTagClicked: (String) -> Unit,
    onFilterClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueWatching: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    changeAnimeSkipIntro: (() -> Unit)?,

    // For bottom action menu
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,

    // Episode selection
    onEpisodeSelected: (EpisodeItem, Boolean, Boolean, Boolean) -> Unit,
    onAllEpisodeSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
) {
    val episodeListState = rememberLazyListState()

    val episodes = remember(state) { state.processedEpisodes.toList() }

    val internalOnBackPressed = {
        if (episodes.fastAny { it.selected }) {
            onAllEpisodeSelected(false)
        } else {
            onBackClicked()
        }
    }
    BackHandler(onBack = internalOnBackPressed)
    Scaffold(
        topBar = {
            val firstVisibleItemIndex by remember {
                derivedStateOf { episodeListState.firstVisibleItemIndex }
            }
            val firstVisibleItemScrollOffset by remember {
                derivedStateOf { episodeListState.firstVisibleItemScrollOffset }
            }
            val animatedTitleAlpha by animateFloatAsState(
                if (firstVisibleItemIndex > 0) 1f else 0f,
            )
            val animatedBgAlpha by animateFloatAsState(
                if (firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0) 1f else 0f,
            )
            EntryToolbar(
                title = state.anime.title,
                titleAlphaProvider = { animatedTitleAlpha },
                backgroundAlphaProvider = { animatedBgAlpha },
                hasFilters = state.anime.episodesFiltered(),
                onBackClicked = internalOnBackPressed,
                onClickFilter = onFilterClicked,
                onClickShare = onShareClicked,
                onClickDownload = onDownloadActionClicked,
                onClickEditCategory = onEditCategoryClicked,
                onClickMigrate = onMigrateClicked,
                changeAnimeSkipIntro = changeAnimeSkipIntro,
                actionModeCounter = episodes.count { it.selected },
                onSelectAll = { onAllEpisodeSelected(true) },
                onInvertSelection = { onInvertSelection() },
                isManga = false,
            )
        },
        bottomBar = {
            SharedAnimeBottomActionMenu(
                selected = episodes.filter { it.selected },
                onEpisodeClicked = onEpisodeClicked,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
                onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
                onDownloadEpisode = onDownloadEpisode,
                onMultiDeleteClicked = onMultiDeleteClicked,
                fillFraction = 1f,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = episodes.fastAny { !it.episode.seen } && episodes.fastAll { !it.selected },
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    text = {
                        val id = if (episodes.fastAny { it.episode.seen }) {
                            R.string.action_resume
                        } else {
                            R.string.action_start
                        }
                        Text(text = stringResource(id))
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                    },
                    onClick = onContinueWatching,
                    expanded = episodeListState.isScrollingUp() || episodeListState.isScrolledToEnd(),
                )
            }
        },
    ) { contentPadding ->
        val topPadding = contentPadding.calculateTopPadding()

        PullRefresh(
            refreshing = state.isRefreshingData,
            onRefresh = onRefresh,
            enabled = episodes.fastAll { !it.selected },
            indicatorPadding = contentPadding,
        ) {
            val layoutDirection = LocalLayoutDirection.current
            VerticalFastScroller(
                listState = episodeListState,
                topContentPadding = topPadding,
                endContentPadding = contentPadding.calculateEndPadding(layoutDirection),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    state = episodeListState,
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        bottom = contentPadding.calculateBottomPadding(),
                    ),
                ) {
                    item(
                        key = EntryScreenItem.INFO_BOX,
                        contentType = EntryScreenItem.INFO_BOX,
                    ) {
                        AnimeInfoBox(
                            isTabletUi = false,
                            appBarPadding = topPadding,
                            title = state.anime.title,
                            author = state.anime.author,
                            artist = state.anime.artist,
                            sourceName = remember { state.source.getNameForAnimeInfo() },
                            isStubSource = remember { state.source is AnimeSourceManager.StubAnimeSource },
                            coverDataProvider = { state.anime },
                            status = state.anime.status,
                            onCoverClick = onCoverClicked,
                            doSearch = onSearch,
                        )
                    }

                    item(
                        key = EntryScreenItem.ACTION_ROW,
                        contentType = EntryScreenItem.ACTION_ROW,
                    ) {
                        AnimeActionRow(
                            favorite = state.anime.favorite,
                            trackingCount = state.trackingCount,
                            onAddToLibraryClicked = onAddToLibraryClicked,
                            onWebViewClicked = onWebViewClicked,
                            onWebViewLongClicked = onWebViewLongClicked,
                            onTrackingClicked = onTrackingClicked,
                            onEditCategory = onEditCategoryClicked,
                        )
                    }

                    item(
                        key = EntryScreenItem.DESCRIPTION_WITH_TAG,
                        contentType = EntryScreenItem.DESCRIPTION_WITH_TAG,
                    ) {
                        ExpandableAnimeDescription(
                            defaultExpandState = state.isFromSource,
                            description = state.anime.description,
                            tagsProvider = { state.anime.genre },
                            onTagClicked = onTagClicked,
                        )
                    }

                    item(
                        key = EntryScreenItem.ITEM_HEADER,
                        contentType = EntryScreenItem.ITEM_HEADER,
                    ) {
                        ItemHeader(
                            enabled = episodes.fastAll { !it.selected },
                            itemCount = episodes.size,
                            onClick = onFilterClicked,
                            isManga = false,
                        )
                    }

                    sharedEpisodeItems(
                        episodes = episodes,
                        onEpisodeClicked = onEpisodeClicked,
                        onDownloadEpisode = onDownloadEpisode,
                        onEpisodeSelected = onEpisodeSelected,
                    )
                }
            }
        }
    }
}

@Composable
fun AnimeScreenLargeImpl(
    state: AnimeScreenState.Success,
    snackbarHostState: SnackbarHostState,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,
    onTagClicked: (String) -> Unit,
    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueWatching: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    changeAnimeSkipIntro: (() -> Unit)?,

    // For bottom action menu
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,

    // Episode selection
    onEpisodeSelected: (EpisodeItem, Boolean, Boolean, Boolean) -> Unit,
    onAllEpisodeSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    val episodes = remember(state) { state.processedEpisodes.toList() }

    val insetPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
    var topBarHeight by remember { mutableStateOf(0) }

    PullRefresh(
        refreshing = state.isRefreshingData,
        onRefresh = onRefresh,
        enabled = episodes.fastAll { !it.selected },
        indicatorPadding = PaddingValues(
            start = insetPadding.calculateStartPadding(layoutDirection),
            top = with(density) { topBarHeight.toDp() },
            end = insetPadding.calculateEndPadding(layoutDirection),
        ),
    ) {
        val episodeListState = rememberLazyListState()

        val internalOnBackPressed = {
            if (episodes.fastAny { it.selected }) {
                onAllEpisodeSelected(false)
            } else {
                onBackClicked()
            }
        }
        BackHandler(onBack = internalOnBackPressed)

        Scaffold(
            topBar = {
                EntryToolbar(
                    modifier = Modifier.onSizeChanged { topBarHeight = (it.height) },
                    title = state.anime.title,
                    titleAlphaProvider = { if (episodes.fastAny { it.selected }) 1f else 0f },
                    backgroundAlphaProvider = { 1f },
                    hasFilters = state.anime.episodesFiltered(),
                    onBackClicked = internalOnBackPressed,
                    onClickFilter = onFilterButtonClicked,
                    onClickShare = onShareClicked,
                    onClickDownload = onDownloadActionClicked,
                    onClickEditCategory = onEditCategoryClicked,
                    onClickMigrate = onMigrateClicked,
                    changeAnimeSkipIntro = changeAnimeSkipIntro,
                    actionModeCounter = episodes.count { it.selected },
                    onSelectAll = { onAllEpisodeSelected(true) },
                    onInvertSelection = { onInvertSelection() },
                    isManga = false,
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    SharedAnimeBottomActionMenu(
                        selected = episodes.filter { it.selected },
                        onEpisodeClicked = onEpisodeClicked,
                        onMultiBookmarkClicked = onMultiBookmarkClicked,
                        onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
                        onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
                        onDownloadEpisode = onDownloadEpisode,
                        onMultiDeleteClicked = onMultiDeleteClicked,
                        fillFraction = 0.5f,
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = episodes.fastAny { !it.episode.seen } && episodes.fastAll { !it.selected },
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    ExtendedFloatingActionButton(
                        text = {
                            val id = if (episodes.fastAny { it.episode.seen }) {
                                R.string.action_resume
                            } else {
                                R.string.action_start
                            }
                            Text(text = stringResource(id))
                        },
                        icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null) },
                        onClick = onContinueWatching,
                        expanded = episodeListState.isScrollingUp() || episodeListState.isScrolledToEnd(),
                    )
                }
            },
        ) { contentPadding ->
            TwoPanelBox(
                modifier = Modifier.padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                ),
                startContent = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = contentPadding.calculateBottomPadding()),
                    ) {
                        AnimeInfoBox(
                            isTabletUi = true,
                            appBarPadding = contentPadding.calculateTopPadding(),
                            title = state.anime.title,
                            author = state.anime.author,
                            artist = state.anime.artist,
                            sourceName = remember { state.source.getNameForAnimeInfo() },
                            isStubSource = remember { state.source is AnimeSourceManager.StubAnimeSource },
                            coverDataProvider = { state.anime },
                            status = state.anime.status,
                            onCoverClick = onCoverClicked,
                            doSearch = onSearch,
                        )
                        AnimeActionRow(
                            favorite = state.anime.favorite,
                            trackingCount = state.trackingCount,
                            onAddToLibraryClicked = onAddToLibraryClicked,
                            onWebViewClicked = onWebViewClicked,
                            onWebViewLongClicked = onWebViewLongClicked,
                            onTrackingClicked = onTrackingClicked,
                            onEditCategory = onEditCategoryClicked,
                        )
                        ExpandableAnimeDescription(
                            defaultExpandState = true,
                            description = state.anime.description,
                            tagsProvider = { state.anime.genre },
                            onTagClicked = onTagClicked,
                        )
                    }
                },
                endContent = {
                    VerticalFastScroller(
                        listState = episodeListState,
                        topContentPadding = contentPadding.calculateTopPadding(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(),
                            state = episodeListState,
                            contentPadding = PaddingValues(
                                top = contentPadding.calculateTopPadding(),
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                        ) {
                            item(
                                key = EntryScreenItem.ITEM_HEADER,
                                contentType = EntryScreenItem.ITEM_HEADER,
                            ) {
                                ItemHeader(
                                    enabled = episodes.fastAll { !it.selected },
                                    itemCount = episodes.size,
                                    onClick = onFilterButtonClicked,
                                    isManga = false,
                                )
                            }

                            sharedEpisodeItems(
                                episodes = episodes,
                                onEpisodeClicked = onEpisodeClicked,
                                onDownloadEpisode = onDownloadEpisode,
                                onEpisodeSelected = onEpisodeSelected,
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SharedAnimeBottomActionMenu(
    selected: List<EpisodeItem>,
    modifier: Modifier = Modifier,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
    onMultiDeleteClicked: (List<Episode>) -> Unit,
    fillFraction: Float,
) {
    val preferences: PlayerPreferences = Injekt.get()
    EntryBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = modifier.fillMaxWidth(fillFraction),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.fastMap { it.episode }, true)
        }.takeIf { selected.fastAny { !it.episode.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.fastMap { it.episode }, false)
        }.takeIf { selected.fastAll { it.episode.bookmark } },
        onMarkAsViewedClicked = {
            onMultiMarkAsSeenClicked(selected.fastMap { it.episode }, true)
        }.takeIf { selected.fastAny { !it.episode.seen } },
        onMarkAsUnviewedClicked = {
            onMultiMarkAsSeenClicked(selected.fastMap { it.episode }, false)
        }.takeIf { selected.fastAny { it.episode.seen || it.episode.lastSecondSeen > 0L } },
        onMarkPreviousAsViewedClicked = {
            onMarkPreviousAsSeenClicked(selected[0].episode)
        }.takeIf { selected.size == 1 },
        onDownloadClicked = {
            onDownloadEpisode!!(selected.toList(), EpisodeDownloadAction.START)
        }.takeIf {
            onDownloadEpisode != null && selected.fastAny { it.downloadState != AnimeDownload.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected.fastMap { it.episode })
        }.takeIf {
            onDownloadEpisode != null && selected.fastAny { it.downloadState == AnimeDownload.State.DOWNLOADED }
        },
        onExternalClicked = {
            onEpisodeClicked(selected.fastMap { it.episode }.first(), true)
        }.takeIf { !preferences.alwaysUseExternalPlayer().get() && selected.size == 1 },
        onInternalClicked = {
            onEpisodeClicked(selected.fastMap { it.episode }.first(), true)
        }.takeIf { preferences.alwaysUseExternalPlayer().get() && selected.size == 1 },
        isManga = false,
    )
}

private fun LazyListScope.sharedEpisodeItems(
    episodes: List<EpisodeItem>,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
    onEpisodeSelected: (EpisodeItem, Boolean, Boolean, Boolean) -> Unit,
) {
    items(
        items = episodes,
        key = { "episode-${it.episode.id}" },
        contentType = { EntryScreenItem.ITEM },
    ) { episodeItem ->
        val haptic = LocalHapticFeedback.current

        AnimeEpisodeListItem(
            title = episodeItem.episodeTitleString,
            date = episodeItem.dateUploadString,
            watchProgress = episodeItem.seenProgressString,
            scanlator = episodeItem.episode.scanlator.takeIf { !it.isNullOrBlank() },
            seen = episodeItem.episode.seen,
            bookmark = episodeItem.episode.bookmark,
            selected = episodeItem.selected,
            downloadIndicatorEnabled = episodes.fastAll { !it.selected },
            downloadStateProvider = { episodeItem.downloadState },
            downloadProgressProvider = { episodeItem.downloadProgress },
            onLongClick = {
                onEpisodeSelected(episodeItem, !episodeItem.selected, true, true)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onClick = {
                onEpisodeItemClick(
                    episodeItem = episodeItem,
                    episodes = episodes,
                    onToggleSelection = { onEpisodeSelected(episodeItem, !episodeItem.selected, true, false) },
                    onEpisodeClicked = onEpisodeClicked,
                )
            },
            onDownloadClick = if (onDownloadEpisode != null) {
                { onDownloadEpisode(listOf(episodeItem), it) }
            } else {
                null
            },
        )
    }
}

private fun onEpisodeItemClick(
    episodeItem: EpisodeItem,
    episodes: List<EpisodeItem>,
    onToggleSelection: (Boolean) -> Unit,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
) {
    when {
        episodeItem.selected -> onToggleSelection(false)
        episodes.fastAny { it.selected } -> onToggleSelection(true)
        else -> onEpisodeClicked(episodeItem.episode, false)
    }
}
