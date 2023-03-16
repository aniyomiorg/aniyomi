package eu.kanade.tachiyomi.ui.updates.anime

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.updates.UpdatesDeleteConfirmationDialog
import eu.kanade.presentation.updates.anime.AnimeUpdateScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.ExternalIntents
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.flow.collectLatest
import uy.kohesive.injekt.injectLazy

@Composable
fun Screen.animeUpdatesTab(
    context: Context,
    fromMore: Boolean,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { AnimeUpdatesScreenModel() }
    val scope = rememberCoroutineScope()
    val state by screenModel.state.collectAsState()

    val navigateUp: (() -> Unit)? = if (fromMore) navigator::pop else null

    fun openEpisodeInternal(context: Context, animeId: Long, episodeId: Long) {
        context.startActivity(PlayerActivity.newIntent(context, animeId, episodeId))
    }

    suspend fun openEpisodeExternal(context: Context, animeId: Long, episodeId: Long) {
        context.startActivity(ExternalIntents.newIntent(context, animeId, episodeId))
    }

    suspend fun openEpisode(updateItem: AnimeUpdatesItem, altPlayer: Boolean = false) {
        val playerPreferences: PlayerPreferences by injectLazy()
        val update = updateItem.update
        if (playerPreferences.alwaysUseExternalPlayer().get() != altPlayer) {
            openEpisodeExternal(context, update.animeId, update.episodeId)
        } else {
            openEpisodeInternal(context, update.animeId, update.episodeId)
        }
    }

    return TabContent(
        titleRes = R.string.label_animeupdates,
        searchEnabled = false,
        content = { contentPadding, _ ->
            AnimeUpdateScreen(
                state = state,
                snackbarHostState = screenModel.snackbarHostState,
                contentPadding = contentPadding,
                lastUpdated = screenModel.lastUpdated,
                relativeTime = screenModel.relativeTime,
                onClickCover = { item -> navigator.push(AnimeScreen(item.update.animeId)) },
                onSelectAll = screenModel::toggleAllSelection,
                onInvertSelection = screenModel::invertSelection,
                onUpdateLibrary = screenModel::updateLibrary,
                onDownloadEpisode = screenModel::downloadEpisodes,
                onMultiBookmarkClicked = screenModel::bookmarkUpdates,
                onMultiMarkAsSeenClicked = screenModel::markUpdatesSeen,
                onMultiDeleteClicked = screenModel::showConfirmDeleteEpisodes,
                onUpdateSelected = screenModel::toggleSelection,
                onOpenEpisode = { updateItem: AnimeUpdatesItem, altPlayer: Boolean ->
                    scope.launchIO {
                        openEpisode(updateItem, altPlayer)
                    }
                    Unit
                },
            )

            val onDismissDialog = { screenModel.setDialog(null) }
            when (val dialog = state.dialog) {
                is AnimeUpdatesScreenModel.Dialog.DeleteConfirmation -> {
                    UpdatesDeleteConfirmationDialog(
                        onDismissRequest = onDismissDialog,
                        onConfirm = { screenModel.deleteEpisodes(dialog.toDelete) },
                        isManga = false,
                    )
                }
                null -> {}
            }

            LaunchedEffect(Unit) {
                screenModel.events.collectLatest { event ->
                    when (event) {
                        AnimeUpdatesScreenModel.Event.InternalError -> screenModel.snackbarHostState.showSnackbar(
                            context.getString(
                                R.string.internal_error,
                            ),
                        )
                        is AnimeUpdatesScreenModel.Event.LibraryUpdateTriggered -> {
                            val msg = if (event.started) {
                                R.string.updating_library
                            } else {
                                R.string.update_already_running
                            }
                            screenModel.snackbarHostState.showSnackbar(context.getString(msg))
                        }
                    }
                }
            }

            LaunchedEffect(state.selectionMode) {
                HomeScreen.showBottomNav(!state.selectionMode)
            }

            LaunchedEffect(state.isLoading) {
                if (!state.isLoading) {
                    (context as? MainActivity)?.ready = true
                }
            }
            DisposableEffect(Unit) {
                screenModel.resetNewUpdatesCount()

                onDispose {
                    screenModel.resetNewUpdatesCount()
                }
            }
        },
        actions =
        if (screenModel.state.collectAsState().value.selected.isNotEmpty()) {
            listOf(
                AppBar.Action(
                    title = stringResource(R.string.action_select_all),
                    icon = Icons.Outlined.SelectAll,
                    onClick = { screenModel.toggleAllSelection(true) },
                ),
                AppBar.Action(
                    title = stringResource(R.string.action_select_inverse),
                    icon = Icons.Outlined.FlipToBack,
                    onClick = { screenModel.invertSelection() },
                ),
            )
        } else {
            listOf(
                AppBar.Action(
                    title = stringResource(R.string.action_update_library),
                    icon = Icons.Outlined.Refresh,
                    onClick = { screenModel.updateLibrary() },
                ),
            )
        },
        navigateUp = navigateUp,
    )
}
