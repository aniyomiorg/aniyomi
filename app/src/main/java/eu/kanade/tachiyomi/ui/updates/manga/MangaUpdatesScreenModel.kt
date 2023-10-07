package eu.kanade.tachiyomi.ui.updates.manga

import android.app.Application
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.preference.asState
import eu.kanade.core.util.addOrRemove
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.items.chapter.interactor.SetReadStatus
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.entries.manga.components.ChapterDownloadAction
import eu.kanade.presentation.updates.manga.MangaUpdatesUiModel
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadCache
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import eu.kanade.tachiyomi.util.lang.toDateKey
import eu.kanade.tachiyomi.util.lang.toRelativeString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.manga.interactor.GetManga
import tachiyomi.domain.items.chapter.interactor.GetChapter
import tachiyomi.domain.items.chapter.interactor.UpdateChapter
import tachiyomi.domain.items.chapter.model.ChapterUpdate
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.manga.service.MangaSourceManager
import tachiyomi.domain.updates.manga.interactor.GetMangaUpdates
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar
import java.util.Date

class MangaUpdatesScreenModel(
    private val sourceManager: MangaSourceManager = Injekt.get(),
    private val downloadManager: MangaDownloadManager = Injekt.get(),
    private val downloadCache: MangaDownloadCache = Injekt.get(),
    private val updateChapter: UpdateChapter = Injekt.get(),
    private val setReadStatus: SetReadStatus = Injekt.get(),
    private val getUpdates: GetMangaUpdates = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val getChapter: GetChapter = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
    uiPreferences: UiPreferences = Injekt.get(),
) : StateScreenModel<UpdatesState>(UpdatesState()) {

    private val _events: Channel<Event> = Channel(Int.MAX_VALUE)
    val events: Flow<Event> = _events.receiveAsFlow()

    val lastUpdated by libraryPreferences.libraryUpdateLastTimestamp().asState(coroutineScope)
    val relativeTime by uiPreferences.relativeTime().asState(coroutineScope)

    // First and last selected index in list
    private val selectedPositions: Array<Int> = arrayOf(-1, -1)
    private val selectedChapterIds: HashSet<Long> = HashSet()

    init {
        coroutineScope.launchIO {
            // Set date limit for recent chapters
            val calendar = Calendar.getInstance().apply {
                time = Date()
                add(Calendar.MONTH, -3)
            }

            combine(
                getUpdates.subscribe(calendar).distinctUntilChanged(),
                downloadCache.changes,
            ) { updates, _ -> updates }
                .catch {
                    logcat(LogPriority.ERROR, it)
                    _events.send(Event.InternalError)
                }
                .collectLatest { updates ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            items = updates.toUpdateItems(),
                        )
                    }
                }
        }

        coroutineScope.launchIO {
            merge(downloadManager.statusFlow(), downloadManager.progressFlow())
                .catch { logcat(LogPriority.ERROR, it) }
                .collect(this@MangaUpdatesScreenModel::updateDownloadState)
        }
    }

    private fun List<MangaUpdatesWithRelations>.toUpdateItems(): List<MangaUpdatesItem> {
        return this.map { update ->
            val activeDownload = downloadManager.getQueuedDownloadOrNull(update.chapterId)
            val downloaded = downloadManager.isChapterDownloaded(
                update.chapterName,
                update.scanlator,
                update.mangaTitle,
                update.sourceId,
            )
            val downloadState = when {
                activeDownload != null -> activeDownload.status
                downloaded -> MangaDownload.State.DOWNLOADED
                else -> MangaDownload.State.NOT_DOWNLOADED
            }
            MangaUpdatesItem(
                update = update,
                downloadStateProvider = { downloadState },
                downloadProgressProvider = { activeDownload?.progress ?: 0 },
                selected = update.chapterId in selectedChapterIds,
            )
        }
    }

    fun updateLibrary(): Boolean {
        val started = MangaLibraryUpdateJob.startNow(Injekt.get<Application>())
        coroutineScope.launch {
            _events.send(Event.LibraryUpdateTriggered(started))
        }
        return started
    }

    /**
     * Update status of chapters.
     *
     * @param download download object containing progress.
     */
    private fun updateDownloadState(download: MangaDownload) {
        mutableState.update { state ->
            val newItems = state.items.toMutableList().apply {
                val modifiedIndex = indexOfFirst { it.update.chapterId == download.chapter.id }
                if (modifiedIndex < 0) return@apply

                val item = get(modifiedIndex)
                set(
                    modifiedIndex,
                    item.copy(
                        downloadStateProvider = { download.status },
                        downloadProgressProvider = { download.progress },
                    ),
                )
            }
            state.copy(items = newItems)
        }
    }

    fun downloadChapters(items: List<MangaUpdatesItem>, action: ChapterDownloadAction) {
        if (items.isEmpty()) return
        coroutineScope.launch {
            when (action) {
                ChapterDownloadAction.START -> {
                    downloadChapters(items)
                    if (items.any { it.downloadStateProvider() == MangaDownload.State.ERROR }) {
                        downloadManager.startDownloads()
                    }
                }
                ChapterDownloadAction.START_NOW -> {
                    val chapterId = items.singleOrNull()?.update?.chapterId ?: return@launch
                    startDownloadingNow(chapterId)
                }
                ChapterDownloadAction.CANCEL -> {
                    val chapterId = items.singleOrNull()?.update?.chapterId ?: return@launch
                    cancelDownload(chapterId)
                }
                ChapterDownloadAction.DELETE -> {
                    deleteChapters(items)
                }
            }
            toggleAllSelection(false)
        }
    }

    private fun startDownloadingNow(chapterId: Long) {
        downloadManager.startDownloadNow(chapterId)
    }

    private fun cancelDownload(chapterId: Long) {
        val activeDownload = downloadManager.getQueuedDownloadOrNull(chapterId) ?: return
        downloadManager.cancelQueuedDownloads(listOf(activeDownload))
        updateDownloadState(activeDownload.apply { status = MangaDownload.State.NOT_DOWNLOADED })
    }

    /**
     * Mark the selected updates list as read/unread.
     * @param updates the list of selected updates.
     * @param read whether to mark chapters as read or unread.
     */
    fun markUpdatesRead(updates: List<MangaUpdatesItem>, read: Boolean) {
        coroutineScope.launchIO {
            setReadStatus.await(
                read = read,
                chapters = updates
                    .mapNotNull { getChapter.await(it.update.chapterId) }
                    .toTypedArray(),
            )
        }
        toggleAllSelection(false)
    }

    /**
     * Bookmarks the given list of chapters.
     * @param updates the list of chapters to bookmark.
     */
    fun bookmarkUpdates(updates: List<MangaUpdatesItem>, bookmark: Boolean) {
        coroutineScope.launchIO {
            updates
                .filterNot { it.update.bookmark == bookmark }
                .map { ChapterUpdate(id = it.update.chapterId, bookmark = bookmark) }
                .let { updateChapter.awaitAll(it) }
        }
        toggleAllSelection(false)
    }

    /**
     * Downloads the given list of chapters with the manager.
     * @param updatesItem the list of chapters to download.
     */
    private fun downloadChapters(updatesItem: List<MangaUpdatesItem>) {
        coroutineScope.launchNonCancellable {
            val groupedUpdates = updatesItem.groupBy { it.update.mangaId }.values
            for (updates in groupedUpdates) {
                val mangaId = updates.first().update.mangaId
                val manga = getManga.await(mangaId) ?: continue
                // Don't download if source isn't available
                sourceManager.get(manga.source) ?: continue
                val chapters = updates.mapNotNull { getChapter.await(it.update.chapterId) }
                downloadManager.downloadChapters(manga, chapters)
            }
        }
    }

    /**
     * Delete selected chapters
     *
     * @param updatesItem list of chapters
     */
    fun deleteChapters(updatesItem: List<MangaUpdatesItem>) {
        coroutineScope.launchNonCancellable {
            updatesItem
                .groupBy { it.update.mangaId }
                .entries
                .forEach { (mangaId, updates) ->
                    val manga = getManga.await(mangaId) ?: return@forEach
                    val source = sourceManager.get(manga.source) ?: return@forEach
                    val chapters = updates.mapNotNull { getChapter.await(it.update.chapterId) }
                    downloadManager.deleteChapters(chapters, manga, source)
                }
        }
        toggleAllSelection(false)
    }

    fun showConfirmDeleteChapters(updatesItem: List<MangaUpdatesItem>) {
        setDialog(Dialog.DeleteConfirmation(updatesItem))
    }

    fun toggleSelection(
        item: MangaUpdatesItem,
        selected: Boolean,
        userSelected: Boolean = false,
        fromLongPress: Boolean = false,
    ) {
        mutableState.update { state ->
            val newItems = state.items.toMutableList().apply {
                val selectedIndex = indexOfFirst { it.update.chapterId == item.update.chapterId }
                if (selectedIndex < 0) return@apply

                val selectedItem = get(selectedIndex)
                if (selectedItem.selected == selected) return@apply

                val firstSelection = none { it.selected }
                set(selectedIndex, selectedItem.copy(selected = selected))
                selectedChapterIds.addOrRemove(item.update.chapterId, selected)

                if (selected && userSelected && fromLongPress) {
                    if (firstSelection) {
                        selectedPositions[0] = selectedIndex
                        selectedPositions[1] = selectedIndex
                    } else {
                        // Try to select the items in-between when possible
                        val range: IntRange
                        if (selectedIndex < selectedPositions[0]) {
                            range = selectedIndex + 1 until selectedPositions[0]
                            selectedPositions[0] = selectedIndex
                        } else if (selectedIndex > selectedPositions[1]) {
                            range = (selectedPositions[1] + 1) until selectedIndex
                            selectedPositions[1] = selectedIndex
                        } else {
                            // Just select itself
                            range = IntRange.EMPTY
                        }

                        range.forEach {
                            val inbetweenItem = get(it)
                            if (!inbetweenItem.selected) {
                                selectedChapterIds.add(inbetweenItem.update.chapterId)
                                set(it, inbetweenItem.copy(selected = true))
                            }
                        }
                    }
                } else if (userSelected && !fromLongPress) {
                    if (!selected) {
                        if (selectedIndex == selectedPositions[0]) {
                            selectedPositions[0] = indexOfFirst { it.selected }
                        } else if (selectedIndex == selectedPositions[1]) {
                            selectedPositions[1] = indexOfLast { it.selected }
                        }
                    } else {
                        if (selectedIndex < selectedPositions[0]) {
                            selectedPositions[0] = selectedIndex
                        } else if (selectedIndex > selectedPositions[1]) {
                            selectedPositions[1] = selectedIndex
                        }
                    }
                }
            }
            state.copy(items = newItems)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        mutableState.update { state ->
            val newItems = state.items.map {
                selectedChapterIds.addOrRemove(it.update.chapterId, selected)
                it.copy(selected = selected)
            }
            state.copy(items = newItems)
        }

        selectedPositions[0] = -1
        selectedPositions[1] = -1
    }

    fun invertSelection() {
        mutableState.update { state ->
            val newItems = state.items.map {
                selectedChapterIds.addOrRemove(it.update.chapterId, !it.selected)
                it.copy(selected = !it.selected)
            }
            state.copy(items = newItems)
        }
        selectedPositions[0] = -1
        selectedPositions[1] = -1
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    fun resetNewUpdatesCount() {
        libraryPreferences.newMangaUpdatesCount().set(0)
    }

    sealed class Dialog {
        data class DeleteConfirmation(val toDelete: List<MangaUpdatesItem>) : Dialog()
    }

    sealed class Event {
        object InternalError : Event()
        data class LibraryUpdateTriggered(val started: Boolean) : Event()
    }
}

@Immutable
data class UpdatesState(
    val isLoading: Boolean = true,
    val items: List<MangaUpdatesItem> = emptyList(),
    val dialog: MangaUpdatesScreenModel.Dialog? = null,
) {
    val selected = items.filter { it.selected }
    val selectionMode = selected.isNotEmpty()

    fun getUiModel(context: Context, relativeTime: Int): List<MangaUpdatesUiModel> {
        val dateFormat by mutableStateOf(UiPreferences.dateFormat(Injekt.get<UiPreferences>().dateFormat().get()))

        return items
            .map { MangaUpdatesUiModel.Item(it) }
            .insertSeparators { before, after ->
                val beforeDate = before?.item?.update?.dateFetch?.toDateKey() ?: Date(0)
                val afterDate = after?.item?.update?.dateFetch?.toDateKey() ?: Date(0)
                when {
                    beforeDate.time != afterDate.time && afterDate.time != 0L -> {
                        val text = afterDate.toRelativeString(
                            context = context,
                            range = relativeTime,
                            dateFormat = dateFormat,
                        )
                        MangaUpdatesUiModel.Header(text)
                    }
                    // Return null to avoid adding a separator between two items.
                    else -> null
                }
            }
    }
}

@Immutable
data class MangaUpdatesItem(
    val update: MangaUpdatesWithRelations,
    val downloadStateProvider: () -> MangaDownload.State,
    val downloadProgressProvider: () -> Int,
    val selected: Boolean = false,
)
