package eu.kanade.tachiyomi.ui.history.anime

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.history.HistoryDeleteAllDialog
import eu.kanade.presentation.history.HistoryDeleteDialog
import eu.kanade.presentation.history.anime.AnimeHistoryScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.domain.items.episode.model.Episode
import uy.kohesive.injekt.injectLazy

val resumeLastEpisodeSeenEvent = Channel<Unit>()

@Composable
fun Screen.animeHistoryTab(
    context: Context,
    fromMore: Boolean,
): TabContent {
    val snackbarHostState = SnackbarHostState()

    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { AnimeHistoryScreenModel() }
    val state by screenModel.state.collectAsState()
    val searchQuery by screenModel.query.collectAsState()

    suspend fun openEpisode(context: Context, episode: Episode?) {
        val playerPreferences: PlayerPreferences by injectLazy()
        val extPlayer = playerPreferences.alwaysUseExternalPlayer().get()
        if (episode != null) {
            MainActivity.startPlayerActivity(context, episode.animeId, episode.id, extPlayer)
        } else {
            snackbarHostState.showSnackbar(context.getString(R.string.no_next_episode))
        }
    }

    val navigateUp: (() -> Unit)? = if (fromMore) navigator::pop else null

    return TabContent(
        titleRes = R.string.label_anime_history,
        searchEnabled = true,
        content = { contentPadding, _ ->
            AnimeHistoryScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = searchQuery,
                snackbarHostState = snackbarHostState,
                onClickCover = { navigator.push(AnimeScreen(it)) },
                onClickResume = screenModel::getNextEpisodeForAnime,
                onDialogChange = screenModel::setDialog,
            )

            val onDismissRequest = { screenModel.setDialog(null) }
            when (val dialog = state.dialog) {
                is AnimeHistoryScreenModel.Dialog.Delete -> {
                    HistoryDeleteDialog(
                        onDismissRequest = onDismissRequest,
                        onDelete = { all ->
                            if (all) {
                                screenModel.removeAllFromHistory(dialog.history.animeId)
                            } else {
                                screenModel.removeFromHistory(dialog.history)
                            }
                        },
                        isManga = false,
                    )
                }
                is AnimeHistoryScreenModel.Dialog.DeleteAll -> {
                    HistoryDeleteAllDialog(
                        onDismissRequest = onDismissRequest,
                        onDelete = screenModel::removeAllHistory,
                    )
                }
                null -> {}
            }

            LaunchedEffect(state.list) {
                if (state.list != null) {
                    (context as? MainActivity)?.ready = true
                }
            }

            LaunchedEffect(Unit) {
                screenModel.events.collectLatest { e ->
                    when (e) {
                        AnimeHistoryScreenModel.Event.InternalError ->
                            snackbarHostState.showSnackbar(context.getString(R.string.internal_error))
                        AnimeHistoryScreenModel.Event.HistoryCleared ->
                            snackbarHostState.showSnackbar(context.getString(R.string.clear_history_completed))
                        is AnimeHistoryScreenModel.Event.OpenEpisode -> openEpisode(context, e.episode)
                    }
                }
            }

            LaunchedEffect(Unit) {
                resumeLastEpisodeSeenEvent.receiveAsFlow().collectLatest {
                    openEpisode(context, screenModel.getNextEpisode())
                }
            }
        },
        actions =
        listOf(
            AppBar.Action(
                title = stringResource(R.string.pref_clear_history),
                icon = Icons.Outlined.DeleteSweep,
                onClick = { screenModel.setDialog(AnimeHistoryScreenModel.Dialog.DeleteAll) },
            ),
        ),
        navigateUp = navigateUp,
    )
}
