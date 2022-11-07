package eu.kanade.presentation.animebrowse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import eu.kanade.data.episode.NoEpisodesException
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.animesource.interactor.GetRemoteAnime
import eu.kanade.domain.library.model.LibraryDisplayMode
import eu.kanade.presentation.animebrowse.components.BrowseAnimeSourceComfortableGrid
import eu.kanade.presentation.animebrowse.components.BrowseAnimeSourceCompactGrid
import eu.kanade.presentation.animebrowse.components.BrowseAnimeSourceList
import eu.kanade.presentation.animebrowse.components.BrowseAnimeSourceToolbar
import eu.kanade.presentation.components.AppStateBanners
import eu.kanade.presentation.components.Divider
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.EmptyScreenAction
import eu.kanade.presentation.components.ExtendedFloatingActionButton
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.Scaffold
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourcePresenter
import eu.kanade.tachiyomi.ui.more.MoreController

@Composable
fun BrowseAnimeSourceScreen(
    presenter: BrowseAnimeSourcePresenter,
    navigateUp: () -> Unit,
    openFilterSheet: () -> Unit,
    onAnimeClick: (Anime) -> Unit,
    onAnimeLongClick: (Anime) -> Unit,
    onWebViewClick: () -> Unit,
    incognitoMode: Boolean,
    downloadedOnlyMode: Boolean,
) {
    val columns by presenter.getColumnsPreferenceForCurrentOrientation()

    val animeList = presenter.getAnimeList().collectAsLazyPagingItems()

    val snackbarHostState = remember { SnackbarHostState() }

    val uriHandler = LocalUriHandler.current

    val onHelpClick = {
        uriHandler.openUri(LocalAnimeSource.HELP_URL)
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                BrowseAnimeSourceToolbar(
                    state = presenter,
                    source = presenter.source,
                    displayMode = presenter.displayMode,
                    onDisplayModeChange = { presenter.displayMode = it },
                    navigateUp = navigateUp,
                    onWebViewClick = onWebViewClick,
                    onHelpClick = onHelpClick,
                    onSearch = { presenter.search(it) },
                )

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = presenter.currentFilter == BrowseAnimeSourcePresenter.AnimeFilter.Popular,
                        onClick = {
                            presenter.reset()
                            presenter.search(GetRemoteAnime.QUERY_POPULAR)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Favorite,
                                contentDescription = "",
                                modifier = Modifier
                                    .size(FilterChipDefaults.IconSize),
                            )
                        },
                        label = {
                            Text(text = stringResource(R.string.popular))
                        },
                    )
                    if (presenter.source?.supportsLatest == true) {
                        FilterChip(
                            selected = presenter.currentFilter == BrowseAnimeSourcePresenter.AnimeFilter.Latest,
                            onClick = {
                                presenter.reset()
                                presenter.search(GetRemoteAnime.QUERY_LATEST)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.NewReleases,
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(FilterChipDefaults.IconSize),
                                )
                            },
                            label = {
                                Text(text = stringResource(R.string.latest))
                            },
                        )
                    }
                    if (presenter.filters.isNotEmpty()) {
                        FilterChip(
                            selected = presenter.currentFilter is BrowseAnimeSourcePresenter.AnimeFilter.UserInput,
                            onClick = openFilterSheet,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.FilterList,
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(FilterChipDefaults.IconSize),
                                )
                            },
                            label = {
                                Text(text = stringResource(R.string.action_filter))
                            },
                        )
                    }
                }

                Divider()

                AppStateBanners(downloadedOnlyMode, incognitoMode)
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { paddingValues ->
        BrowseAnimeSourceContent(
            state = presenter,
            animeList = animeList,
            getAnimeState = { presenter.getAnime(it) },
            columns = columns,
            displayMode = presenter.displayMode,
            snackbarHostState = snackbarHostState,
            contentPadding = paddingValues,
            onWebViewClick = onWebViewClick,
            onHelpClick = { uriHandler.openUri(MoreController.URL_HELP) },
            onLocalAnimeSourceHelpClick = onHelpClick,
            onAnimeClick = onAnimeClick,
            onAnimeLongClick = onAnimeLongClick,
        )
    }
}

@Composable
fun BrowseAnimeSourceFloatingActionButton(
    modifier: Modifier = Modifier.navigationBarsPadding(),
    isVisible: Boolean,
    onFabClick: () -> Unit,
) {
    AnimatedVisibility(visible = isVisible) {
        ExtendedFloatingActionButton(
            modifier = modifier,
            text = { Text(text = stringResource(R.string.action_filter)) },
            icon = { Icon(Icons.Outlined.FilterList, contentDescription = "") },
            onClick = onFabClick,
        )
    }
}

@Composable
fun BrowseAnimeSourceContent(
    state: BrowseAnimeSourceState,
    animeList: LazyPagingItems<Anime>,
    getAnimeState: @Composable ((Anime) -> State<Anime>),
    columns: GridCells,
    displayMode: LibraryDisplayMode,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLocalAnimeSourceHelpClick: () -> Unit,
    onAnimeClick: (Anime) -> Unit,
    onAnimeLongClick: (Anime) -> Unit,
) {
    val context = LocalContext.current

    val errorState = animeList.loadState.refresh.takeIf { it is LoadState.Error }
        ?: animeList.loadState.append.takeIf { it is LoadState.Error }

    val getErrorMessage: (LoadState.Error) -> String = { state ->
        when {
            state.error is NoEpisodesException -> context.getString(R.string.no_results_found)
            state.error.message.isNullOrEmpty() -> ""
            state.error.message.orEmpty().startsWith("HTTP error") -> "${state.error.message}: ${context.getString(R.string.http_error_hint)}"
            else -> state.error.message.orEmpty()
        }
    }

    LaunchedEffect(errorState) {
        if (animeList.itemCount > 0 && errorState != null && errorState is LoadState.Error) {
            val result = snackbarHostState.showSnackbar(
                message = getErrorMessage(errorState),
                actionLabel = context.getString(R.string.action_webview_refresh),
                duration = SnackbarDuration.Indefinite,
            )
            when (result) {
                SnackbarResult.Dismissed -> snackbarHostState.currentSnackbarData?.dismiss()
                SnackbarResult.ActionPerformed -> animeList.refresh()
            }
        }
    }

    if (animeList.itemCount <= 0 && errorState != null && errorState is LoadState.Error) {
        EmptyScreen(
            message = getErrorMessage(errorState),
            actions = if (state.source is LocalAnimeSource) {
                listOf(
                    EmptyScreenAction(
                        stringResId = R.string.local_source_help_guide,
                        icon = Icons.Outlined.HelpOutline,
                        onClick = onLocalAnimeSourceHelpClick,
                    ),
                )
            } else {
                listOf(
                    EmptyScreenAction(
                        stringResId = R.string.action_retry,
                        icon = Icons.Outlined.Refresh,
                        onClick = animeList::refresh,
                    ),
                    EmptyScreenAction(
                        stringResId = R.string.action_open_in_web_view,
                        icon = Icons.Outlined.Public,
                        onClick = onWebViewClick,
                    ),
                    EmptyScreenAction(
                        stringResId = R.string.label_help,
                        icon = Icons.Outlined.HelpOutline,
                        onClick = onHelpClick,
                    ),
                )
            },
        )

        return
    }

    if (animeList.itemCount == 0 && animeList.loadState.refresh is LoadState.Loading) {
        LoadingScreen()
        return
    }

    when (displayMode) {
        LibraryDisplayMode.ComfortableGrid -> {
            BrowseAnimeSourceComfortableGrid(
                animeList = animeList,
                getAnimeState = getAnimeState,
                columns = columns,
                contentPadding = contentPadding,
                onAnimeClick = onAnimeClick,
                onAnimeLongClick = onAnimeLongClick,
            )
        }
        LibraryDisplayMode.List -> {
            BrowseAnimeSourceList(
                animeList = animeList,
                getAnimeState = getAnimeState,
                contentPadding = contentPadding,
                onAnimeClick = onAnimeClick,
                onAnimeLongClick = onAnimeLongClick,
            )
        }
        else -> {
            BrowseAnimeSourceCompactGrid(
                animeList = animeList,
                getAnimeState = getAnimeState,
                columns = columns,
                contentPadding = contentPadding,
                onAnimeClick = onAnimeClick,
                onAnimeLongClick = onAnimeLongClick,
            )
        }
    }
}
