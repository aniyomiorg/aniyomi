package eu.kanade.tachiyomi.ui.updates.manga

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
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.updates.UpdatesDeleteConfirmationDialog
import eu.kanade.presentation.updates.manga.MangaUpdateScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.coroutines.flow.collectLatest

@Composable
fun Screen.mangaUpdatesTab(
    context: Context,
    fromMore: Boolean,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { MangaUpdatesScreenModel() }
    val state by screenModel.state.collectAsState()

    val navigateUp: (() -> Unit)? = if (fromMore) navigator::pop else null

    return TabContent(
        titleRes = R.string.label_updates,
        searchEnabled = false,
        content = { contentPadding, _ ->
            MangaUpdateScreen(
                state = state,
                snackbarHostState = screenModel.snackbarHostState,
                contentPadding = contentPadding,
                lastUpdated = screenModel.lastUpdated,
                relativeTime = screenModel.relativeTime,
                onClickCover = { item -> navigator.push(MangaScreen(item.update.mangaId)) },
                onSelectAll = screenModel::toggleAllSelection,
                onInvertSelection = screenModel::invertSelection,
                onUpdateLibrary = screenModel::updateLibrary,
                onDownloadChapter = screenModel::downloadChapters,
                onMultiBookmarkClicked = screenModel::bookmarkUpdates,
                onMultiMarkAsReadClicked = screenModel::markUpdatesRead,
                onMultiDeleteClicked = screenModel::showConfirmDeleteChapters,
                onUpdateSelected = screenModel::toggleSelection,
                onOpenChapter = {
                    val intent =
                        ReaderActivity.newIntent(context, it.update.mangaId, it.update.chapterId)
                    context.startActivity(intent)
                },
            )

            val onDismissDialog = { screenModel.setDialog(null) }
            when (val dialog = state.dialog) {
                is MangaUpdatesScreenModel.Dialog.DeleteConfirmation -> {
                    UpdatesDeleteConfirmationDialog(
                        onDismissRequest = onDismissDialog,
                        onConfirm = { screenModel.deleteChapters(dialog.toDelete) },
                        isManga = true,
                    )
                }
                null -> {}
            }

            LaunchedEffect(Unit) {
                screenModel.events.collectLatest { event ->
                    when (event) {
                        MangaUpdatesScreenModel.Event.InternalError -> screenModel.snackbarHostState.showSnackbar(
                            context.getString(
                                R.string.internal_error,
                            ),
                        )
                        is MangaUpdatesScreenModel.Event.LibraryUpdateTriggered -> {
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
