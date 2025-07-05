package eu.kanade.tachiyomi.ui.history.manga

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.entries.manga.DuplicateMangaDialog
import eu.kanade.presentation.history.HistoryDeleteAllDialog
import eu.kanade.presentation.history.HistoryDeleteDialog
import eu.kanade.presentation.history.manga.MangaHistoryScreen
import eu.kanade.tachiyomi.ui.browse.manga.migration.search.MigrateMangaDialog
import eu.kanade.tachiyomi.ui.browse.manga.migration.search.MigrateMangaDialogScreenModel
import eu.kanade.tachiyomi.ui.category.CategoriesTab
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.stringResource

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
            snackbarHostState.showSnackbar(context.stringResource(MR.strings.no_next_chapter))
        }
    }

    val scope = rememberCoroutineScope()
    val navigateUp: (() -> Unit)? = if (fromMore) {
        {
            if (navigator.lastItem == HomeScreen) {
                scope.launch { HomeScreen.openTab(HomeScreen.Tab.AnimeLib()) }
            } else {
                navigator.pop()
            }
        }
    } else {
        null
    }

    return TabContent(
        titleRes = AYMR.strings.label_history,
        searchEnabled = true,
        content = { contentPadding, _ ->
            MangaHistoryScreen(
                state = state,
                searchQuery = searchQuery,
                snackbarHostState = snackbarHostState,
                onClickCover = { navigator.push(MangaScreen(it)) },
                onClickResume = screenModel::getNextChapterForManga,
                onDialogChange = screenModel::setDialog,
                onClickFavorite = screenModel::addFavorite,
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
                is MangaHistoryScreenModel.Dialog.DuplicateManga -> {
                    DuplicateMangaDialog(
                        onDismissRequest = onDismissRequest,
                        onConfirm = {
                            screenModel.addFavorite(dialog.manga)
                        },
                        onOpenManga = { navigator.push(MangaScreen(dialog.duplicate.id)) },
                        onMigrate = {
                            screenModel.showMigrateDialog(dialog.manga, dialog.duplicate)
                        },
                    )
                }
                is MangaHistoryScreenModel.Dialog.ChangeCategory -> {
                    ChangeCategoryDialog(
                        initialSelection = dialog.initialSelection,
                        onDismissRequest = onDismissRequest,
                        onEditCategories = {
                            navigator.push(CategoriesTab)
                            CategoriesTab.showMangaCategory()
                        },
                        onConfirm = { include, _ ->
                            screenModel.moveMangaToCategoriesAndAddToLibrary(dialog.manga, include)
                        },
                    )
                }
                is MangaHistoryScreenModel.Dialog.Migrate -> {
                    MigrateMangaDialog(
                        oldManga = dialog.oldManga,
                        newManga = dialog.newManga,
                        screenModel = MigrateMangaDialogScreenModel(),
                        onDismissRequest = onDismissRequest,
                        onClickTitle = { navigator.push(MangaScreen(dialog.oldManga.id)) },
                        onPopScreen = { navigator.replace(MangaScreen(dialog.newManga.id)) },
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
                            snackbarHostState.showSnackbar(context.stringResource(MR.strings.internal_error))
                        MangaHistoryScreenModel.Event.HistoryCleared ->
                            snackbarHostState.showSnackbar(context.stringResource(MR.strings.clear_history_completed))
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
        persistentListOf(
            AppBar.Action(
                title = stringResource(MR.strings.pref_clear_history),
                icon = Icons.Outlined.DeleteSweep,
                onClick = { screenModel.setDialog(MangaHistoryScreenModel.Dialog.DeleteAll) },
            ),
        ),
        navigateUp = navigateUp,
    )
}
