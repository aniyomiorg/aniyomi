package eu.kanade.tachiyomi.ui.history.manga

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
import eu.kanade.presentation.history.manga.MangaHistoryScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.domain.items.chapter.model.Chapter

val resumeLastChapterReadEvent = Channel<Unit>()

@Composable
fun Screen.mangaHistoryTab(
    context: Context,
    fromMore: Boolean,
): TabContent {
    val snackbarHostState = SnackbarHostState()

    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { MangaHistoryScreenModel() }
    val state by screenModel.state.collectAsState()
    val searchQuery by screenModel.query.collectAsState()

    suspend fun openChapter(context: Context, chapter: Chapter?) {
        if (chapter != null) {
            val intent = ReaderActivity.newIntent(context, chapter.mangaId, chapter.id)
            context.startActivity(intent)
        } else {
            snackbarHostState.showSnackbar(context.getString(R.string.no_next_chapter))
        }
    }

    val navigateUp: (() -> Unit)? = if (fromMore) navigator::pop else null

    return TabContent(
        titleRes = R.string.label_history,
        searchEnabled = true,
        content = { contentPadding, _ ->
            MangaHistoryScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = searchQuery,
                snackbarHostState = snackbarHostState,
                onClickCover = { navigator.push(MangaScreen(it)) },
                onClickResume = screenModel::getNextChapterForManga,
                onDialogChange = screenModel::setDialog,
            )

            val onDismissRequest = { screenModel.setDialog(null) }
            when (val dialog = state.dialog) {
                is MangaHistoryScreenModel.Dialog.Delete -> {
                    HistoryDeleteDialog(
                        onDismissRequest = onDismissRequest,
                        onDelete = { all ->
                            if (all) {
                                screenModel.removeAllFromHistory(dialog.history.mangaId)
                            } else {
                                screenModel.removeFromHistory(dialog.history)
                            }
                        },
                        isManga = true,
                    )
                }
                is MangaHistoryScreenModel.Dialog.DeleteAll -> {
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
                        MangaHistoryScreenModel.Event.InternalError ->
                            snackbarHostState.showSnackbar(context.getString(R.string.internal_error))
                        MangaHistoryScreenModel.Event.HistoryCleared ->
                            snackbarHostState.showSnackbar(context.getString(R.string.clear_history_completed))
                        is MangaHistoryScreenModel.Event.OpenChapter -> openChapter(context, e.chapter)
                    }
                }
            }

            LaunchedEffect(Unit) {
                resumeLastChapterReadEvent.receiveAsFlow().collectLatest {
                    openChapter(context, screenModel.getNextChapter())
                }
            }
        },
        actions =
        listOf(
            AppBar.Action(
                title = stringResource(R.string.pref_clear_history),
                icon = Icons.Outlined.DeleteSweep,
                onClick = { screenModel.setDialog(MangaHistoryScreenModel.Dialog.DeleteAll) },
            ),
        ),
        navigateUp = navigateUp,
    )
}
