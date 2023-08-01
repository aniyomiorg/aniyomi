package eu.kanade.presentation.browse.anime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.browse.anime.components.BrowseAnimeSourceComfortableGrid
import eu.kanade.presentation.browse.anime.components.BrowseAnimeSourceCompactGrid
import eu.kanade.presentation.browse.anime.components.BrowseAnimeSourceList
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.anime.model.StubAnimeSource
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.entries.anime.LocalAnimeSource

@Composable
fun BrowseAnimeSourceContent(
    source: AnimeSource?,
    animeList: LazyPagingItems<StateFlow<Anime>>,
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
        with(context) { state.error.formattedMessage }
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
            modifier = Modifier.padding(contentPadding),
            message = getErrorMessage(errorState),
            actions = if (source is LocalAnimeSource) {
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
        LoadingScreen(
            modifier = Modifier.padding(contentPadding),
        )
        return
    }

    when (displayMode) {
        LibraryDisplayMode.ComfortableGrid -> {
            BrowseAnimeSourceComfortableGrid(
                animeList = animeList,
                columns = columns,
                contentPadding = contentPadding,
                onAnimeClick = onAnimeClick,
                onAnimeLongClick = onAnimeLongClick,
            )
        }
        LibraryDisplayMode.List -> {
            BrowseAnimeSourceList(
                animeList = animeList,
                contentPadding = contentPadding,
                onAnimeClick = onAnimeClick,
                onAnimeLongClick = onAnimeLongClick,
            )
        }
        LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
            BrowseAnimeSourceCompactGrid(
                animeList = animeList,
                columns = columns,
                contentPadding = contentPadding,
                onAnimeClick = onAnimeClick,
                onAnimeLongClick = onAnimeLongClick,
            )
        }
    }
}

@Composable
fun MissingSourceScreen(
    source: StubAnimeSource,
    navigateUp: () -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = source.name,
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        EmptyScreen(
            message = stringResource(R.string.source_not_installed, source.toString()),
            modifier = Modifier.padding(paddingValues),
        )
    }
}
