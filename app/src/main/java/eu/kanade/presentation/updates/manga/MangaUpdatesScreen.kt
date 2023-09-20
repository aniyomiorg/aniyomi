package eu.kanade.presentation.updates.manga

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.entries.EntryBottomActionMenu
import eu.kanade.presentation.entries.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.ui.updates.manga.MangaUpdatesItem
import eu.kanade.tachiyomi.ui.updates.manga.UpdatesState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import kotlin.time.Duration.Companion.seconds

@Composable
fun MangaUpdateScreen(
    state: UpdatesState,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    lastUpdated: Long,
    relativeTime: Int,
    onClickCover: (MangaUpdatesItem) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onUpdateLibrary: () -> Boolean,
    onDownloadChapter: (List<MangaUpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<MangaUpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<MangaUpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<MangaUpdatesItem>) -> Unit,
    onUpdateSelected: (MangaUpdatesItem, Boolean, Boolean, Boolean) -> Unit,
    onOpenChapter: (MangaUpdatesItem) -> Unit,
) {
    BackHandler(enabled = state.selectionMode, onBack = { onSelectAll(false) })

    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            MangaUpdatesBottomBar(
                selected = state.selected,
                onDownloadChapter = onDownloadChapter,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                onMultiDeleteClicked = onMultiDeleteClicked,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) {
        when {
            state.isLoading -> LoadingScreen(modifier = Modifier.padding(contentPadding))
            state.items.isEmpty() -> EmptyScreen(
                textResource = R.string.information_no_recent,
                modifier = Modifier.padding(contentPadding),
            )
            else -> {
                val scope = rememberCoroutineScope()
                var isRefreshing by remember { mutableStateOf(false) }

                PullRefresh(
                    refreshing = isRefreshing,
                    onRefresh = {
                        val started = onUpdateLibrary()
                        if (!started) return@PullRefresh
                        scope.launch {
                            // Fake refresh status but hide it after a second as it's a long running task
                            isRefreshing = true
                            delay(1.seconds)
                            isRefreshing = false
                        }
                    },
                    enabled = !state.selectionMode,
                    indicatorPadding = contentPadding,
                ) {
                    FastScrollLazyColumn(
                        contentPadding = contentPadding,
                    ) {
                        if (lastUpdated > 0L) {
                            mangaUpdatesLastUpdatedItem(lastUpdated)
                        }

                        mangaUpdatesUiItems(
                            uiModels = state.getUiModel(context, relativeTime),
                            selectionMode = state.selectionMode,
                            onUpdateSelected = onUpdateSelected,
                            onClickCover = onClickCover,
                            onClickUpdate = onOpenChapter,
                            onDownloadChapter = onDownloadChapter,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaUpdatesBottomBar(
    selected: List<MangaUpdatesItem>,
    onDownloadChapter: (List<MangaUpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<MangaUpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<MangaUpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<MangaUpdatesItem>) -> Unit,
) {
    EntryBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, true)
        }.takeIf { selected.fastAny { !it.update.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, false)
        }.takeIf { selected.fastAll { it.update.bookmark } },
        onMarkAsViewedClicked = {
            onMultiMarkAsReadClicked(selected, true)
        }.takeIf { selected.fastAny { !it.update.read } },
        onMarkAsUnviewedClicked = {
            onMultiMarkAsReadClicked(selected, false)
        }.takeIf { selected.fastAny { it.update.read || it.update.lastPageRead > 0L } },
        onDownloadClicked = {
            onDownloadChapter(selected, ChapterDownloadAction.START)
        }.takeIf {
            selected.fastAny { it.downloadStateProvider() != MangaDownload.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected)
        }.takeIf { selected.fastAny { it.downloadStateProvider() == MangaDownload.State.DOWNLOADED } },
        isManga = true,
    )
}

sealed class MangaUpdatesUiModel {
    data class Header(val date: String) : MangaUpdatesUiModel()
    data class Item(val item: MangaUpdatesItem) : MangaUpdatesUiModel()
}
