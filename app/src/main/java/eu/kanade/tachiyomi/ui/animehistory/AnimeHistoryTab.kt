package eu.kanade.tachiyomi.ui.animehistory

import android.content.Context
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.episode.model.Episode
import eu.kanade.presentation.animehistory.AnimeHistoryScreen
import eu.kanade.presentation.history.components.HistoryDeleteAllDialog
import eu.kanade.presentation.animehistory.components.AnimeHistoryDeleteDialog
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.setting.PlayerPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import uy.kohesive.injekt.injectLazy

object AnimeHistoryTab : Tab {

    private val snackbarHostState = SnackbarHostState()

    private val resumeLastEpisodeReadEvent = Channel<Unit>()

    private val playerPreferences: PlayerPreferences by injectLazy()

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_history_enter)
            return TabOptions(
                index = 2u,
                title = stringResource(R.string.label_recent_manga),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        resumeLastEpisodeReadEvent.send(Unit)
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { AnimeHistoryScreenModel() }
        val state by screenModel.state.collectAsState()

        AnimeHistoryScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onSearchQueryChange = screenModel::updateSearchQuery,
            onClickCover = { navigator.push(AnimeScreen(it)) },
            onClickResume = screenModel::getNextEpisodeForAnime,
            onDialogChange = screenModel::setDialog,
        )

        val onDismissRequest = { screenModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is AnimeHistoryScreenModel.Dialog.Delete -> {
                AnimeHistoryDeleteDialog(
                    onDismissRequest = onDismissRequest,
                    onDelete = { all ->
                        if (all) {
                            screenModel.removeAllFromHistory(dialog.history.animeId)
                        } else {
                            screenModel.removeFromHistory(dialog.history)
                        }
                    },
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
            resumeLastEpisodeReadEvent.consumeAsFlow().collectLatest {
                openEpisode(context, screenModel.getNextEpisode())
            }
        }
    }

    // TODO: Add external player setting
    suspend fun openEpisode(context: Context, episode: Episode?) {
        if (episode != null) {
            val intent = PlayerActivity.newIntent(context, episode.animeId, episode.id)
            context.startActivity(intent)
        } else {
            snackbarHostState.showSnackbar(context.getString(R.string.no_next_episode))
        }
    }
}
