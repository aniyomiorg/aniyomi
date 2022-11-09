package eu.kanade.presentation.animebrowse

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.paging.compose.collectAsLazyPagingItems
import eu.kanade.domain.anime.model.Anime
import eu.kanade.presentation.browse.BrowseSourceFloatingActionButton
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourcePresenter
import eu.kanade.tachiyomi.ui.more.MoreController

@Composable
fun AnimeSourceSearchScreen(
    presenter: BrowseAnimeSourcePresenter,
    navigateUp: () -> Unit,
    onFabClick: () -> Unit,
    onAnimeClick: (Anime) -> Unit,
    onWebViewClick: () -> Unit,
) {
    val columns by presenter.getColumnsPreferenceForCurrentOrientation()

    val mangaList = presenter.getAnimeList().collectAsLazyPagingItems()

    val snackbarHostState = remember { SnackbarHostState() }

    val uriHandler = LocalUriHandler.current

    val onHelpClick = {
        uriHandler.openUri(LocalAnimeSource.HELP_URL)
    }

    Scaffold(
        topBar = { scrollBehavior ->
            SearchToolbar(
                searchQuery = presenter.searchQuery ?: "",
                onChangeSearchQuery = { presenter.searchQuery = it },
                onClickCloseSearch = navigateUp,
                onSearch = { presenter.search(it) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            BrowseSourceFloatingActionButton(
                isVisible = presenter.filters.isNotEmpty(),
                onFabClick = onFabClick,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { paddingValues ->
        BrowseAnimeSourceContent(
            state = presenter,
            animeList = mangaList,
            getAnimeState = { presenter.getAnime(it) },
            columns = columns,
            displayMode = presenter.displayMode,
            snackbarHostState = snackbarHostState,
            contentPadding = paddingValues,
            onWebViewClick = onWebViewClick,
            onHelpClick = { uriHandler.openUri(MoreController.URL_HELP) },
            onLocalAnimeSourceHelpClick = onHelpClick,
            onAnimeClick = onAnimeClick,
            onAnimeLongClick = onAnimeClick,
        )
    }
}
