package eu.kanade.presentation.browse.anime

import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalContext
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.data.items.episode.NoEpisodesException
import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.domain.library.model.LibraryDisplayMode
import eu.kanade.presentation.browse.anime.components.BrowseAnimeSourceComfortableGrid
import eu.kanade.presentation.browse.anime.components.BrowseAnimeSourceCompactGrid
import eu.kanade.presentation.browse.anime.components.BrowseAnimeSourceList
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.EmptyScreenAction
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.source.anime.LocalAnimeSource
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BrowseAnimeSourceContent(
    source: AnimeCatalogueSource?,
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
        LoadingScreen()
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
