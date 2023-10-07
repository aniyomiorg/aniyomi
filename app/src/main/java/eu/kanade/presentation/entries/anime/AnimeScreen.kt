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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.entries.anime.model.episodesFiltered
import eu.kanade.presentation.entries.DownloadAction
import eu.kanade.presentation.entries.EntryBottomActionMenu
import eu.kanade.presentation.entries.EntryScreenItem
import eu.kanade.presentation.entries.EntryToolbar
import eu.kanade.presentation.entries.ItemHeader
import eu.kanade.presentation.entries.anime.components.AnimeActionRow
import eu.kanade.presentation.entries.anime.components.AnimeEpisodeListItem
import eu.kanade.presentation.entries.anime.components.AnimeInfoBox
import eu.kanade.presentation.entries.anime.components.EpisodeDownloadAction
import eu.kanade.presentation.entries.anime.components.ExpandableAnimeDescription
import eu.kanade.presentation.entries.anime.components.NextEpisodeAiringListItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.source.anime.getNameForAnimeInfo
import eu.kanade.tachiyomi.ui.browse.anime.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreenState
import eu.kanade.tachiyomi.ui.entries.anime.EpisodeItem
import eu.kanade.tachiyomi.ui.entries.anime.episodeDecimalFormat
import eu.kanade.tachiyomi.util.lang.toRelativeString
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.coroutines.delay
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.service.missingItemsCount
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.anime.model.StubAnimeSource
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.components.TwoPanelBox
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.components.material.ExtendedFloatingActionButton
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.util.isScrolledToEnd
import tachiyomi.presentation.core.util.isScrollingUp
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun AnimeScreen(
    state: AnimeScreenState.Success,
    snackbarHostState: SnackbarHostState,
    dateRelativeTime: Int,
    dateFormat: DateFormat,
    isTabletUi: Boolean,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    showNextEpisodeAirTime: Boolean,
    alwaysUseExternalPlayer: Boolean,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (episode: Episode, alt: Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,

    // For tags menu
    onTagSearch: (String) -> Unit,

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

    // For episode swipe
    onEpisodeSwipe: (EpisodeItem, LibraryPreferences.EpisodeSwipeAction) -> Unit,

    // Episode selection
    onEpisodeSelected: (EpisodeItem, Boolean, Boolean, Boolean) -> Unit,
    onAllEpisodeSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
) {
    val context = LocalContext.current
    val onCopyTagToClipboard: (tag: String) -> Unit = {
        if (it.isNotEmpty()) {
            context.copyToClipboard(it, it)
        }
    }

    val navigator = LocalNavigator.currentOrThrow
    val onSettingsClicked: (() -> Unit)? = {
        navigator.push(SourcePreferencesScreen(state.source.id))
    }.takeIf { state.source is ConfigurableAnimeSource }

    if (!isTabletUi) {
        AnimeScreenSmallImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            dateRelativeTime = dateRelativeTime,
            dateFormat = dateFormat,
            episodeSwipeEndAction = episodeSwipeEndAction,
            episodeSwipeStartAction = episodeSwipeStartAction,
            showNextEpisodeAirTime = showNextEpisodeAirTime,
            alwaysUseExternalPlayer = alwaysUseExternalPlayer,
            onBackClicked = onBackClicked,
            onEpisodeClicked = onEpisodeClicked,
            onDownloadEpisode = onDownloadEpisode,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onTagSearch = onTagSearch,
            onCopyTagToClipboard = onCopyTagToClipboard,
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
            onEpisodeSwipe = onEpisodeSwipe,
            onEpisodeSelected = onEpisodeSelected,
            onAllEpisodeSelected = onAllEpisodeSelected,
            onInvertSelection = onInvertSelection,
            onSettingsClicked = onSettingsClicked,
        )
    } else {
        AnimeScreenLargeImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            dateRelativeTime = dateRelativeTime,
            episodeSwipeEndAction = episodeSwipeEndAction,
            episodeSwipeStartAction = episodeSwipeStartAction,
            showNextEpisodeAirTime = showNextEpisodeAirTime,
            alwaysUseExternalPlayer = alwaysUseExternalPlayer,
            dateFormat = dateFormat,
            onBackClicked = onBackClicked,
            onEpisodeClicked = onEpisodeClicked,
            onDownloadEpisode = onDownloadEpisode,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onTagSearch = onTagSearch,
            onCopyTagToClipboard = onCopyTagToClipboard,
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
            onEpisodeSwipe = onEpisodeSwipe,
            onEpisodeSelected = onEpisodeSelected,
            onAllEpisodeSelected = onAllEpisodeSelected,
            onInvertSelection = onInvertSelection,
            onSettingsClicked = onSettingsClicked,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimeScreenSmallImpl(
    state: AnimeScreenState.Success,
    snackbarHostState: SnackbarHostState,
    dateRelativeTime: Int,
    dateFormat: DateFormat,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    showNextEpisodeAirTime: Boolean,
    alwaysUseExternalPlayer: Boolean,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,

    // For tags menu
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,

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
    onSettingsClicked: (() -> Unit)?,

    // For bottom action menu
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,

    // For episode swipe
    onEpisodeSwipe: (EpisodeItem, LibraryPreferences.EpisodeSwipeAction) -> Unit,

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
                onClickRefresh = onRefresh,
                onClickMigrate = onMigrateClicked,
                changeAnimeSkipIntro = changeAnimeSkipIntro,
                actionModeCounter = episodes.count { it.selected },
                onSelectAll = { onAllEpisodeSelected(true) },
                onInvertSelection = { onInvertSelection() },
                isManga = false,
                onClickSettings = onSettingsClicked,
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
                alwaysUseExternalPlayer = alwaysUseExternalPlayer,
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
                            isStubSource = remember { state.source is StubAnimeSource },
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
                            onTagSearch = onTagSearch,
                            onCopyTagToClipboard = onCopyTagToClipboard,
                        )
                    }

                    item(
                        key = EntryScreenItem.ITEM_HEADER,
                        contentType = EntryScreenItem.ITEM_HEADER,
                    ) {
                        ItemHeader(
                            enabled = episodes.fastAll { !it.selected },
                            itemCount = episodes.size,
                            missingItemsCount = episodes.map { it.episode.episodeNumber }.missingItemsCount(),
                            onClick = onFilterClicked,
                            isManga = false,
                        )
                    }

                    if (state.airingTime > 0L) {
                        item(
                            key = EntryScreenItem.AIRING_TIME,
                            contentType = EntryScreenItem.AIRING_TIME,
                        ) {
                            // Handles the second by second countdown
                            var timer by remember { mutableStateOf(state.airingTime) }
                            LaunchedEffect(key1 = timer) {
                                if (timer > 0L) {
                                    delay(1000L)
                                    timer -= 1000L
                                }
                            }
                            if (timer > 0L && showNextEpisodeAirTime && state.anime.status.toInt() != SAnime.COMPLETED) {
                                NextEpisodeAiringListItem(
                                    title = stringResource(
                                        R.string.display_mode_episode,
                                        episodeDecimalFormat.format(state.airingEpisodeNumber),
                                    ),
                                    date = formatTime(state.airingTime, useDayFormat = true),
                                )
                            }
                        }
                    }

                    sharedEpisodeItems(
                        anime = state.anime,
                        episodes = episodes,
                        dateRelativeTime = dateRelativeTime,
                        dateFormat = dateFormat,
                        episodeSwipeEndAction = episodeSwipeEndAction,
                        episodeSwipeStartAction = episodeSwipeStartAction,
                        onEpisodeClicked = onEpisodeClicked,
                        onDownloadEpisode = onDownloadEpisode,
                        onEpisodeSelected = onEpisodeSelected,
                        onEpisodeSwipe = onEpisodeSwipe,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeScreenLargeImpl(
    state: AnimeScreenState.Success,
    snackbarHostState: SnackbarHostState,
    dateRelativeTime: Int,
    dateFormat: DateFormat,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    showNextEpisodeAirTime: Boolean,
    alwaysUseExternalPlayer: Boolean,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,

    // For tags menu
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,

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
    onSettingsClicked: (() -> Unit)?,

    // For bottom action menu
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,

    // For swipe actions
    onEpisodeSwipe: (EpisodeItem, LibraryPreferences.EpisodeSwipeAction) -> Unit,

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
                    onClickRefresh = onRefresh,
                    onClickMigrate = onMigrateClicked,
                    changeAnimeSkipIntro = changeAnimeSkipIntro,
                    actionModeCounter = episodes.count { it.selected },
                    onSelectAll = { onAllEpisodeSelected(true) },
                    onInvertSelection = { onInvertSelection() },
                    isManga = false,
                    onClickSettings = onSettingsClicked,
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
                        alwaysUseExternalPlayer = alwaysUseExternalPlayer,
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
                            isStubSource = remember { state.source is StubAnimeSource },
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
                            onTagSearch = onTagSearch,
                            onCopyTagToClipboard = onCopyTagToClipboard,
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
                                    missingItemsCount = episodes.map { it.episode.episodeNumber }.missingItemsCount(),
                                    onClick = onFilterButtonClicked,
                                    isManga = false,
                                )
                            }

                            if (state.airingTime > 0L) {
                                item(
                                    key = EntryScreenItem.AIRING_TIME,
                                    contentType = EntryScreenItem.AIRING_TIME,
                                ) {
                                    // Handles the second by second countdown
                                    var timer by remember { mutableStateOf(state.airingTime) }
                                    LaunchedEffect(key1 = timer) {
                                        if (timer > 0L) {
                                            delay(1000L)
                                            timer -= 1000L
                                        }
                                    }
                                    if (timer > 0L && showNextEpisodeAirTime && state.anime.status.toInt() != SAnime.COMPLETED) {
                                        NextEpisodeAiringListItem(
                                            title = stringResource(
                                                R.string.display_mode_episode,
                                                episodeDecimalFormat.format(state.airingEpisodeNumber),
                                            ),
                                            date = formatTime(state.airingTime, useDayFormat = true),
                                        )
                                    }
                                }
                            }

                            sharedEpisodeItems(
                                anime = state.anime,
                                episodes = episodes,
                                dateRelativeTime = dateRelativeTime,
                                dateFormat = dateFormat,
                                episodeSwipeEndAction = episodeSwipeEndAction,
                                episodeSwipeStartAction = episodeSwipeStartAction,
                                onEpisodeClicked = onEpisodeClicked,
                                onDownloadEpisode = onDownloadEpisode,
                                onEpisodeSelected = onEpisodeSelected,
                                onEpisodeSwipe = onEpisodeSwipe,
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
    alwaysUseExternalPlayer: Boolean,
) {
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
        }.takeIf { !alwaysUseExternalPlayer && selected.size == 1 },
        onInternalClicked = {
            onEpisodeClicked(selected.fastMap { it.episode }.first(), true)
        }.takeIf { alwaysUseExternalPlayer && selected.size == 1 },
        isManga = false,
    )
}

private fun LazyListScope.sharedEpisodeItems(
    anime: Anime,
    episodes: List<EpisodeItem>,
    dateRelativeTime: Int,
    dateFormat: DateFormat,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
    onEpisodeSelected: (EpisodeItem, Boolean, Boolean, Boolean) -> Unit,
    onEpisodeSwipe: (EpisodeItem, LibraryPreferences.EpisodeSwipeAction) -> Unit,
) {
    items(
        items = episodes,
        key = { "episode-${it.episode.id}" },
        contentType = { EntryScreenItem.ITEM },
    ) { episodeItem ->
        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current

        AnimeEpisodeListItem(
            title = if (anime.displayMode == Anime.EPISODE_DISPLAY_NUMBER) {
                stringResource(
                    R.string.display_mode_episode,
                    episodeDecimalFormat.format(episodeItem.episode.episodeNumber.toDouble()),
                )
            } else {
                episodeItem.episode.name
            },
            date = episodeItem.episode.dateUpload
                .takeIf { it > 0L }
                ?.let {
                    Date(it).toRelativeString(
                        context,
                        dateRelativeTime,
                        dateFormat,
                    )
                },
            watchProgress = episodeItem.episode.lastSecondSeen
                .takeIf { !episodeItem.episode.seen && it > 0L }
                ?.let {
                    stringResource(
                        R.string.episode_progress,
                        formatTime(it),
                        formatTime(episodeItem.episode.totalSeconds),
                    )
                },
            scanlator = episodeItem.episode.scanlator.takeIf { !it.isNullOrBlank() },
            seen = episodeItem.episode.seen,
            bookmark = episodeItem.episode.bookmark,
            selected = episodeItem.selected,
            downloadIndicatorEnabled = episodes.fastAll { !it.selected },
            downloadStateProvider = { episodeItem.downloadState },
            downloadProgressProvider = { episodeItem.downloadProgress },
            episodeSwipeEndAction = episodeSwipeEndAction,
            episodeSwipeStartAction = episodeSwipeStartAction,
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
            onEpisodeSwipe = {
                onEpisodeSwipe(episodeItem, it)
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

private fun formatTime(milliseconds: Long, useDayFormat: Boolean = false): String {
    return if (useDayFormat) {
        String.format(
            "Airing in %02dd %02dh %02dm %02ds",
            TimeUnit.MILLISECONDS.toDays(milliseconds),
            TimeUnit.MILLISECONDS.toHours(milliseconds) -
                TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(milliseconds)),
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    } else if (milliseconds > 3600000L) {
        String.format(
            "%d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(milliseconds),
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    } else {
        String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    }
}
