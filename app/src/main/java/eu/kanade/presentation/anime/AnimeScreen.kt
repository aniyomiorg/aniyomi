package eu.kanade.presentation.anime

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.rememberTopAppBarScrollState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import eu.kanade.domain.anime.model.Anime.Companion.EPISODE_DISPLAY_NUMBER
import eu.kanade.domain.episode.model.Episode
import eu.kanade.presentation.anime.components.AnimeBottomActionMenu
import eu.kanade.presentation.anime.components.AnimeEpisodeListItem
import eu.kanade.presentation.anime.components.AnimeInfoHeader
import eu.kanade.presentation.anime.components.AnimeSmallAppBar
import eu.kanade.presentation.anime.components.AnimeTopAppBar
import eu.kanade.presentation.anime.components.EpisodeHeader
import eu.kanade.presentation.components.ExtendedFloatingActionButton
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.components.SwipeRefreshIndicator
import eu.kanade.presentation.components.VerticalFastScroller
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.manga.EpisodeDownloadAction
import eu.kanade.presentation.util.ExitUntilCollapsedScrollBehavior
import eu.kanade.presentation.util.isScrolledToEnd
import eu.kanade.presentation.util.isScrollingUp
import eu.kanade.presentation.util.plus
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.getNameForAnimeInfo
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.anime.AnimeScreenState
import eu.kanade.tachiyomi.ui.anime.EpisodeItem
import eu.kanade.tachiyomi.util.lang.toRelativeString
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Date
import java.util.concurrent.TimeUnit

private val episodeDecimalFormat = DecimalFormat(
    "#.###",
    DecimalFormatSymbols()
        .apply { decimalSeparator = '.' },
)

@Composable
fun AnimeScreen(
    state: AnimeScreenState.Success,
    snackbarHostState: SnackbarHostState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
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

    // For bottom action menu
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,
) {
    if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
        AnimeScreenSmallImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            onBackClicked = onBackClicked,
            onEpisodeClicked = onEpisodeClicked,
            onDownloadEpisode = onDownloadEpisode,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onTrackingClicked = onTrackingClicked,
            onTagClicked = onTagClicked,
            onFilterButtonClicked = onFilterButtonClicked,
            onRefresh = onRefresh,
            onContinueReading = onContinueWatching,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            onEditCategoryClicked = onEditCategoryClicked,
            onMigrateClicked = onMigrateClicked,
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
            onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
        )
    } else {
        AnimeScreenLargeImpl(
            state = state,
            windowWidthSizeClass = windowWidthSizeClass,
            snackbarHostState = snackbarHostState,
            onBackClicked = onBackClicked,
            onEpisodeClicked = onEpisodeClicked,
            onDownloadEpisode = onDownloadEpisode,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onTrackingClicked = onTrackingClicked,
            onTagClicked = onTagClicked,
            onFilterButtonClicked = onFilterButtonClicked,
            onRefresh = onRefresh,
            onContinueReading = onContinueWatching,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            onEditCategoryClicked = onEditCategoryClicked,
            onMigrateClicked = onMigrateClicked,
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
            onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
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
    onTrackingClicked: (() -> Unit)?,
    onTagClicked: (String) -> Unit,
    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,

    // For bottom action menu
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val scrollBehavior = ExitUntilCollapsedScrollBehavior(rememberTopAppBarScrollState(), decayAnimationSpec)
    val episodeListState = rememberLazyListState()
    SideEffect {
        if (episodeListState.firstVisibleItemIndex > 0 || episodeListState.firstVisibleItemScrollOffset > 0) {
            // Should go here after a configuration change
            // Safe to say that the app bar is fully scrolled
            scrollBehavior.state.offset = scrollBehavior.state.offsetLimit
        }
    }

    val insetPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
    val (topBarHeight, onTopBarHeightChanged) = remember { mutableStateOf(1) }
    SwipeRefresh(
        state = rememberSwipeRefreshState(state.isRefreshingInfo || state.isRefreshingEpisode),
        onRefresh = onRefresh,
        indicatorPadding = PaddingValues(
            start = insetPadding.calculateStartPadding(layoutDirection),
            top = with(LocalDensity.current) { topBarHeight.toDp() },
            end = insetPadding.calculateEndPadding(layoutDirection),
        ),
        indicator = { s, trigger ->
            SwipeRefreshIndicator(
                state = s,
                refreshTriggerDistance = trigger,
            )
        },
    ) {
        val episodes = remember(state) { state.processedEpisodes.toList() }
        val selected = remember(episodes) { emptyList<EpisodeItem>().toMutableStateList() }
        val selectedPositions = remember(episodes) { arrayOf(-1, -1) } // first and last selected index in list

        val internalOnBackPressed = {
            if (selected.isNotEmpty()) {
                selected.clear()
            } else {
                onBackClicked()
            }
        }
        BackHandler(onBack = internalOnBackPressed)

        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(insetPadding),
            topBar = {
                AnimeTopAppBar(
                    modifier = Modifier
                        .scrollable(
                            state = rememberScrollableState {
                                var consumed = runBlocking { episodeListState.scrollBy(-it) } * -1
                                if (consumed == 0f) {
                                    // Pass scroll to app bar if we're on the top of the list
                                    val newOffset =
                                        (scrollBehavior.state.offset + it).coerceIn(scrollBehavior.state.offsetLimit, 0f)
                                    consumed = newOffset - scrollBehavior.state.offset
                                    scrollBehavior.state.offset = newOffset
                                }
                                consumed
                            },
                            orientation = Orientation.Vertical,
                            interactionSource = episodeListState.interactionSource as MutableInteractionSource,
                        ),
                    title = state.anime.title,
                    author = state.anime.author,
                    artist = state.anime.artist,
                    description = state.anime.description,
                    tagsProvider = { state.anime.genre },
                    coverDataProvider = { state.anime },
                    sourceName = remember { state.source.getNameForAnimeInfo() },
                    isStubSource = remember { state.source is SourceManager.StubSource },
                    favorite = state.anime.favorite,
                    status = state.anime.status,
                    trackingCount = state.trackingCount,
                    episodeCount = episodes.size,
                    episodeFiltered = state.anime.episodesFiltered(),
                    incognitoMode = state.isIncognitoMode,
                    downloadedOnlyMode = state.isDownloadedOnlyMode,
                    fromSource = state.isFromSource,
                    onBackClicked = internalOnBackPressed,
                    onCoverClick = onCoverClicked,
                    onTagClicked = onTagClicked,
                    onAddToLibraryClicked = onAddToLibraryClicked,
                    onWebViewClicked = onWebViewClicked,
                    onTrackingClicked = onTrackingClicked,
                    onFilterButtonClicked = onFilterButtonClicked,
                    onShareClicked = onShareClicked,
                    onDownloadClicked = onDownloadActionClicked,
                    onEditCategoryClicked = onEditCategoryClicked,
                    onMigrateClicked = onMigrateClicked,
                    doGlobalSearch = onSearch,
                    scrollBehavior = scrollBehavior,
                    actionModeCounter = selected.size,
                    onSelectAll = {
                        selected.clear()
                        selected.addAll(episodes)
                    },
                    onInvertSelection = {
                        val toSelect = episodes - selected
                        selected.clear()
                        selected.addAll(toSelect)
                    },
                    onSmallAppBarHeightChanged = onTopBarHeightChanged,
                )
            },
            bottomBar = {
                SharedAnimeBottomActionMenu(
                    selected = selected,
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
                    visible = episodes.any { !it.episode.seen } && selected.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    ExtendedFloatingActionButton(
                        text = {
                            val id = if (episodes.any { it.episode.seen }) {
                                R.string.action_resume
                            } else {
                                R.string.action_start
                            }
                            Text(text = stringResource(id))
                        },
                        icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) },
                        onClick = onContinueReading,
                        expanded = episodeListState.isScrollingUp() || episodeListState.isScrolledToEnd(),
                        modifier = Modifier
                            .padding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues()),
                    )
                }
            },
        ) { contentPadding ->
            val withNavBarContentPadding = contentPadding +
                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues()
            VerticalFastScroller(
                listState = episodeListState,
                thumbAllowed = { scrollBehavior.state.offset == scrollBehavior.state.offsetLimit },
                topContentPadding = withNavBarContentPadding.calculateTopPadding(),
                endContentPadding = withNavBarContentPadding.calculateEndPadding(LocalLayoutDirection.current),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    state = episodeListState,
                    contentPadding = withNavBarContentPadding,
                ) {
                    sharedEpisodeItems(
                        episodes = episodes,
                        state = state,
                        selected = selected,
                        selectedPositions = selectedPositions,
                        onEpisodeClicked = onEpisodeClicked,
                        onDownloadEpisode = onDownloadEpisode,
                    )
                }
            }
        }
    }
}

@Composable
fun AnimeScreenLargeImpl(
    state: AnimeScreenState.Success,
    windowWidthSizeClass: WindowWidthSizeClass,
    snackbarHostState: SnackbarHostState,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,
    onTagClicked: (String) -> Unit,
    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,

    // For bottom action menu
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    val insetPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
    val (topBarHeight, onTopBarHeightChanged) = remember { mutableStateOf(0) }
    SwipeRefresh(
        state = rememberSwipeRefreshState(state.isRefreshingInfo || state.isRefreshingEpisode),
        onRefresh = onRefresh,
        indicatorPadding = PaddingValues(
            start = insetPadding.calculateStartPadding(layoutDirection),
            top = with(density) { topBarHeight.toDp() },
            end = insetPadding.calculateEndPadding(layoutDirection),
        ),
        clipIndicatorToPadding = true,
        indicator = { s, trigger ->
            SwipeRefreshIndicator(
                state = s,
                refreshTriggerDistance = trigger,
            )
        },
    ) {
        val episodeListState = rememberLazyListState()
        val episodes = remember(state) { state.processedEpisodes.toList() }
        val selected = remember(episodes) { emptyList<EpisodeItem>().toMutableStateList() }
        val selectedPositions = remember(episodes) { arrayOf(-1, -1) } // first and last selected index in list

        val internalOnBackPressed = {
            if (selected.isNotEmpty()) {
                selected.clear()
            } else {
                onBackClicked()
            }
        }
        BackHandler(onBack = internalOnBackPressed)

        Scaffold(
            modifier = Modifier.padding(insetPadding),
            topBar = {
                AnimeSmallAppBar(
                    modifier = Modifier.onSizeChanged { onTopBarHeightChanged(it.height) },
                    title = state.anime.title,
                    titleAlphaProvider = { if (selected.isEmpty()) 0f else 1f },
                    backgroundAlphaProvider = { 1f },
                    incognitoMode = state.isIncognitoMode,
                    downloadedOnlyMode = state.isDownloadedOnlyMode,
                    onBackClicked = internalOnBackPressed,
                    onShareClicked = onShareClicked,
                    onDownloadClicked = onDownloadActionClicked,
                    onEditCategoryClicked = onEditCategoryClicked,
                    onMigrateClicked = onMigrateClicked,
                    actionModeCounter = selected.size,
                    onSelectAll = {
                        selected.clear()
                        selected.addAll(episodes)
                    },
                    onInvertSelection = {
                        val toSelect = episodes - selected
                        selected.clear()
                        selected.addAll(toSelect)
                    },
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    SharedAnimeBottomActionMenu(
                        selected = selected,
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
                    visible = episodes.any { !it.episode.seen } && selected.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    ExtendedFloatingActionButton(
                        text = {
                            val id = if (episodes.any { it.episode.seen }) {
                                R.string.action_resume
                            } else {
                                R.string.action_start
                            }
                            Text(text = stringResource(id))
                        },
                        icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) },
                        onClick = onContinueReading,
                        expanded = episodeListState.isScrollingUp() || episodeListState.isScrolledToEnd(),
                        modifier = Modifier
                            .padding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues()),
                    )
                }
            },
        ) { contentPadding ->
            Row {
                val withNavBarContentPadding = contentPadding +
                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues()
                AnimeInfoHeader(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = withNavBarContentPadding.calculateBottomPadding()),
                    windowWidthSizeClass = WindowWidthSizeClass.Expanded,
                    appBarPadding = contentPadding.calculateTopPadding(),
                    title = state.anime.title,
                    author = state.anime.author,
                    artist = state.anime.artist,
                    description = state.anime.description,
                    tagsProvider = { state.anime.genre },
                    sourceName = remember { state.source.getNameForAnimeInfo() },
                    isStubSource = remember { state.source is AnimeSourceManager.StubSource },
                    coverDataProvider = { state.anime },
                    favorite = state.anime.favorite,
                    status = state.anime.status,
                    trackingCount = state.trackingCount,
                    fromSource = state.isFromSource,
                    onAddToLibraryClicked = onAddToLibraryClicked,
                    onWebViewClicked = onWebViewClicked,
                    onTrackingClicked = onTrackingClicked,
                    onTagClicked = onTagClicked,
                    onEditCategory = onEditCategoryClicked,
                    onCoverClick = onCoverClicked,
                    doSearch = onSearch,
                )

                val episodesWeight = if (windowWidthSizeClass == WindowWidthSizeClass.Medium) 1f else 2f
                VerticalFastScroller(
                    listState = episodeListState,
                    modifier = Modifier.weight(episodesWeight),
                    topContentPadding = withNavBarContentPadding.calculateTopPadding(),
                    endContentPadding = withNavBarContentPadding.calculateEndPadding(layoutDirection),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(),
                        state = episodeListState,
                        contentPadding = withNavBarContentPadding,
                    ) {
                        item(contentType = "header") {
                            EpisodeHeader(
                                episodeCount = episodes.size,
                                isEpisodeFiltered = state.anime.episodesFiltered(),
                                onFilterButtonClicked = onFilterButtonClicked,
                            )
                        }

                        sharedEpisodeItems(
                            episodes = episodes,
                            state = state,
                            selected = selected,
                            selectedPositions = selectedPositions,
                            onEpisodeClicked = onEpisodeClicked,
                            onDownloadEpisode = onDownloadEpisode,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedAnimeBottomActionMenu(
    selected: SnapshotStateList<EpisodeItem>,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
    onMultiDeleteClicked: (List<Episode>) -> Unit,
    fillFraction: Float,
) {
    val preferences: PreferencesHelper = Injekt.get()
    AnimeBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(fillFraction),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.map { it.episode }, true)
            selected.clear()
        }.takeIf { selected.any { !it.episode.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.map { it.episode }, false)
            selected.clear()
        }.takeIf { selected.all { it.episode.bookmark } },
        onMarkAsSeenClicked = {
            onMultiMarkAsSeenClicked(selected.map { it.episode }, true)
            selected.clear()
        }.takeIf { selected.any { !it.episode.seen } },
        onMarkAsUnseenClicked = {
            onMultiMarkAsSeenClicked(selected.map { it.episode }, false)
            selected.clear()
        }.takeIf { selected.any { it.episode.seen } },
        onMarkPreviousAsSeenClicked = {
            onMarkPreviousAsSeenClicked(selected[0].episode)
            selected.clear()
        }.takeIf { selected.size == 1 },
        onDownloadClicked = {
            onDownloadEpisode!!(selected.toList(), EpisodeDownloadAction.START)
            selected.clear()
        }.takeIf {
            onDownloadEpisode != null && selected.any { it.downloadState != AnimeDownload.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected.map { it.episode })
            selected.clear()
        }.takeIf {
            onDownloadEpisode != null && selected.any { it.downloadState == AnimeDownload.State.DOWNLOADED }
        },
        onExternalClicked = {
            onEpisodeClicked(selected.map { it.episode }.first(), true)
            selected.clear()
        }.takeIf { !preferences.alwaysUseExternalPlayer() && selected.size == 1 },
        onInternalClicked = {
            onEpisodeClicked(selected.map { it.episode }.first(), true)
            selected.clear()
        }.takeIf { preferences.alwaysUseExternalPlayer() && selected.size == 1 },
    )
}

private fun LazyListScope.sharedEpisodeItems(
    episodes: List<EpisodeItem>,
    state: AnimeScreenState.Success,
    selected: SnapshotStateList<EpisodeItem>,
    selectedPositions: Array<Int>,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeItem>, EpisodeDownloadAction) -> Unit)?,
) {
    items(items = episodes) { episodeItem ->
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current

        val (episode, downloadState, downloadProgress) = episodeItem
        val episodeTitle = if (state.anime.displayMode == EPISODE_DISPLAY_NUMBER) {
            stringResource(
                id = R.string.display_mode_episode,
                episodeDecimalFormat.format(episode.episodeNumber.toDouble()),
            )
        } else {
            episode.name
        }
        val date = remember(episode.dateUpload) {
            episode.dateUpload
                .takeIf { it > 0 }
                ?.let {
                    Date(it).toRelativeString(
                        context,
                        state.dateRelativeTime,
                        state.dateFormat,
                    )
                }
        }
        val lastSecondSeen = remember(episode.lastSecondSeen, episode.seen) {
            episode.lastSecondSeen.takeIf { !episode.seen && it > 0 }
        }
        val totalSeconds = remember(episode.totalSeconds) {
            episode.totalSeconds.takeIf { !episode.seen && it > 0 }
        }
        val scanlator = remember(episode.scanlator) { episode.scanlator.takeIf { !it.isNullOrBlank() } }

        AnimeEpisodeListItem(
            title = episodeTitle,
            date = date,
            watchProgress = lastSecondSeen?.let {
                if (totalSeconds != null) {
                    stringResource(
                        id = R.string.episode_progress,
                        formatProgress(lastSecondSeen),
                        formatProgress(totalSeconds),
                    )
                } else {
                    stringResource(
                        id = R.string.episode_progress_no_total,
                        formatProgress(lastSecondSeen),
                    )
                }
            },
            scanlator = scanlator,
            seen = episode.seen,
            bookmark = episode.bookmark,
            selected = selected.contains(episodeItem),
            downloadState = downloadState,
            downloadProgress = downloadProgress,
            onLongClick = {
                val dispatched = onEpisodeItemLongClick(
                    episodeItem = episodeItem,
                    selected = selected,
                    episodes = episodes,
                    selectedPositions = selectedPositions,
                )
                if (dispatched) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onClick = {
                onEpisodeItemClick(
                    episodeItem = episodeItem,
                    selected = selected,
                    episodes = episodes,
                    selectedPositions = selectedPositions,
                    onEpisodeClicked = onEpisodeClicked,
                )
            },
            onDownloadClick = if (onDownloadEpisode != null) {
                { onDownloadEpisode(listOf(episodeItem), it) }
            } else null,
        )
    }
}

private fun onEpisodeItemLongClick(
    episodeItem: EpisodeItem,
    selected: MutableList<EpisodeItem>,
    episodes: List<EpisodeItem>,
    selectedPositions: Array<Int>,
): Boolean {
    if (!selected.contains(episodeItem)) {
        val selectedIndex = episodes.indexOf(episodeItem)
        if (selected.isEmpty()) {
            selected.add(episodeItem)
            selectedPositions[0] = selectedIndex
            selectedPositions[1] = selectedIndex
            return true
        }

        // Try to select the items in-between when possible
        val range: IntRange
        if (selectedIndex < selectedPositions[0]) {
            range = selectedIndex until selectedPositions[0]
            selectedPositions[0] = selectedIndex
        } else if (selectedIndex > selectedPositions[1]) {
            range = (selectedPositions[1] + 1)..selectedIndex
            selectedPositions[1] = selectedIndex
        } else {
            // Just select itself
            range = selectedIndex..selectedIndex
        }

        range.forEach {
            val toAdd = episodes[it]
            if (!selected.contains(toAdd)) {
                selected.add(toAdd)
            }
        }
        return true
    }
    return false
}

private fun onEpisodeItemClick(
    episodeItem: EpisodeItem,
    selected: MutableList<EpisodeItem>,
    episodes: List<EpisodeItem>,
    selectedPositions: Array<Int>,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
) {
    val selectedIndex = episodes.indexOf(episodeItem)
    when {
        selected.contains(episodeItem) -> {
            val removedIndex = episodes.indexOf(episodeItem)
            selected.remove(episodeItem)

            if (removedIndex == selectedPositions[0]) {
                selectedPositions[0] = episodes.indexOfFirst { selected.contains(it) }
            } else if (removedIndex == selectedPositions[1]) {
                selectedPositions[1] = episodes.indexOfLast { selected.contains(it) }
            }
        }
        selected.isNotEmpty() -> {
            if (selectedIndex < selectedPositions[0]) {
                selectedPositions[0] = selectedIndex
            } else if (selectedIndex > selectedPositions[1]) {
                selectedPositions[1] = selectedIndex
            }
            selected.add(episodeItem)
        }
        else -> onEpisodeClicked(episodeItem.episode, false)
    }
}

private fun formatProgress(milliseconds: Long): String {
    return if (milliseconds > 3600000L) String.format(
        "%d:%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(milliseconds),
        TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
        TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
    ) else {
        String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    }
}
