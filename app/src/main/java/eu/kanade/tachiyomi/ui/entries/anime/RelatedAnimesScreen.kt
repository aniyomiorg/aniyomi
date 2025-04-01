package eu.kanade.tachiyomi.ui.entries.anime

import android.content.res.Configuration
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.core.preference.asState
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.anime.RelatedAnimesContent
import eu.kanade.presentation.browse.anime.components.BrowseSourceSimpleToolbar
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.BrowseAnimeSourceScreen
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreen
import kotlinx.coroutines.CoroutineScope
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.presentation.core.components.material.Scaffold
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun RelatedAnimesScreen(
    screenModel: AnimeScreenModel,
    navigateUp: () -> Unit,
    navigator: Navigator,
    scope: CoroutineScope,
    successState: AnimeScreenModel.State.Success,
) {
    val sourcePreferences: SourcePreferences = Injekt.get()
    var displayMode by sourcePreferences.sourceDisplayMode().asState(scope)

    val haptic = LocalHapticFeedback.current

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = { scrollBehavior ->
            BrowseSourceSimpleToolbar(
                navigateUp = navigateUp,
                title = successState.anime.title,
                displayMode = displayMode,
                onDisplayModeChange = { displayMode = it },
                scrollBehavior = scrollBehavior,
                toggleSelectionMode = {},
                isRunning = false,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        RelatedAnimesContent(
            relatedAnimes = successState.relatedAnimesSorted,
            getMangaState = { manga -> screenModel.getManga(initialManga = manga) },
            columns = getColumnsPreference(LocalConfiguration.current.orientation),
            displayMode = displayMode,
            contentPadding = paddingValues,
            onMangaClick = {
                scope.launchIO {
                    val manga = screenModel.networkToLocalAnime.getLocal(it)
                    navigator.push(AnimeScreen(manga.id, true))
                }
            },
            onMangaLongClick = {
                scope.launchIO {
                    val manga = screenModel.networkToLocalAnime.getLocal(it)
                    navigator.push(AnimeScreen(manga.id, true))
                }
            },
            onKeywordClick = { query ->
                navigator.push(BrowseAnimeSourceScreen(successState.source.id, query))
            },
            onKeywordLongClick = { query ->
                navigator.push(GlobalAnimeSearchScreen(query))
            },
            selection = emptyList(),
        )
    }
}

private fun getColumnsPreference(orientation: Int): GridCells {
    val libraryPreferences: LibraryPreferences = Injekt.get()

    val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) {
        libraryPreferences.animeLandscapeColumns()
    } else {
        libraryPreferences.animePortraitColumns()
    }.get()
    return if (columns == 0) GridCells.Adaptive(128.dp) else GridCells.Fixed(columns)
}
