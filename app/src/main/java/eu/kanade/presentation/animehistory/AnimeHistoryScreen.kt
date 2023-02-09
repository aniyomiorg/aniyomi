package eu.kanade.presentation.animehistory

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import eu.kanade.presentation.animehistory.components.AnimeHistoryContent
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.animehistory.AnimeHistoryScreenModel
import eu.kanade.tachiyomi.ui.animehistory.AnimeHistoryState
import java.util.Date

@Composable
fun AnimeHistoryScreen(
    state: AnimeHistoryState,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String?) -> Unit,
    onClickCover: (animeId: Long) -> Unit,
    onClickResume: (animeId: Long, episodeId: Long) -> Unit,
    onDialogChange: (AnimeHistoryScreenModel.Dialog?) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            SearchToolbar(
                titleContent = { AppBarTitle(stringResource(R.string.history)) },
                searchQuery = state.searchQuery,
                onChangeSearchQuery = onSearchQueryChange,
                actions = {
                    IconButton(onClick = { onDialogChange(AnimeHistoryScreenModel.Dialog.DeleteAll) }) {
                        Icon(
                            Icons.Outlined.DeleteSweep,
                            contentDescription = stringResource(R.string.pref_clear_history),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(modifier = Modifier.padding(contentPadding))
            } else if (it.isEmpty()) {
                val msg = if (!state.searchQuery.isNullOrEmpty()) {
                    R.string.no_results_found
                } else {
                    R.string.information_no_recent_anime
                }
                EmptyScreen(
                    textResource = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                AnimeHistoryContent(
                    history = it,
                    contentPadding = contentPadding,
                    onClickCover = { history -> onClickCover(history.animeId) },
                    onClickResume = { history -> onClickResume(history.animeId, history.episodeId) },
                    onClickDelete = { item -> onDialogChange(AnimeHistoryScreenModel.Dialog.Delete(item)) },
                )
            }
        }
    }
}

sealed class AnimeHistoryUiModel {
    data class Header(val date: Date) : AnimeHistoryUiModel()
    data class Item(val item: AnimeHistoryWithRelations) : AnimeHistoryUiModel()
}
