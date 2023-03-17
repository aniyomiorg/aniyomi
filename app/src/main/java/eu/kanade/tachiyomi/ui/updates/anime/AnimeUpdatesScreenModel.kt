package eu.kanade.tachiyomi.ui.updates.anime

import android.app.Application
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.prefs.asState
import eu.kanade.core.util.addOrRemove
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.entries.anime.interactor.GetAnime
import eu.kanade.domain.items.episode.interactor.GetEpisode
import eu.kanade.domain.items.episode.interactor.SetSeenStatus
import eu.kanade.domain.items.episode.interactor.UpdateEpisode
import eu.kanade.domain.items.episode.model.EpisodeUpdate
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.updates.anime.interactor.GetAnimeUpdates
import eu.kanade.domain.updates.anime.model.AnimeUpdatesWithRelations
import eu.kanade.presentation.components.EpisodeDownloadAction
import eu.kanade.presentation.updates.anime.AnimeUpdatesUiModel
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadCache
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadService
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateService
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import eu.kanade.tachiyomi.util.lang.toDateKey
import eu.kanade.tachiyomi.util.lang.toRelativeString
import eu.kanade.tachiyomi.util.system.logcat
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
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class AnimeUpdatesScreenModel(
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val downloadManager: AnimeDownloadManager = Injekt.get(),
    private val downloadCache: AnimeDownloadCache = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val setSeenStatus: SetSeenStatus = Injekt.get(),
    private val getUpdates: GetAnimeUpdates = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val getEpisode: GetEpisode = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
    uiPreferences: UiPreferences = Injekt.get(),
) : StateScreenModel<AnimeUpdatesState>(AnimeUpdatesState()) {

    private val _events: Channel<Event> = Channel(Int.MAX_VALUE)
    val events: Flow<Event> = _events.receiveAsFlow()

    val lastUpdated by libraryPreferences.libraryUpdateLastTimestamp().asState(coroutineScope)

    val relativeTime: Int by uiPreferences.relativeTime().asState(coroutineScope)
    val dateFormat: DateFormat by mutableStateOf(UiPreferences.dateFormat(uiPreferences.dateFormat().get()))

    // First and last selected index in list
    private val selectedPositions: Array<Int> = arrayOf(-1, -1)
    private val selectedEpisodeIds: HashSet<Long> = HashSet()

    init {
        coroutineScope.launchIO {
            // Set date limit for recent episodes
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
            merge(downloadManager.queue.statusFlow(), downloadManager.queue.progressFlow())
                .catch { logcat(LogPriority.ERROR, it) }
                .collect(this@AnimeUpdatesScreenModel::updateDownloadState)
        }
    }

    private fun List<AnimeUpdatesWithRelations>.toUpdateItems(): List<AnimeUpdatesItem> {
        return this.map {
            val activeDownload = downloadManager.getQueuedDownloadOrNull(it.episodeId)
            val downloaded = downloadManager.isEpisodeDownloaded(
                it.episodeName,
                it.scanlator,
                it.animeTitle,
                it.sourceId,
            )
            val downloadState = when {
                activeDownload != null -> activeDownload.status
                downloaded -> AnimeDownload.State.DOWNLOADED
                else -> AnimeDownload.State.NOT_DOWNLOADED
            }
            AnimeUpdatesItem(
                update = it,
                downloadStateProvider = { downloadState },
                downloadProgressProvider = { activeDownload?.progress ?: 0 },
                selected = it.episodeId in selectedEpisodeIds,
            )
        }
    }

    fun updateLibrary(): Boolean {
        val started = AnimeLibraryUpdateService.start(Injekt.get<Application>())
        coroutineScope.launch {
            _events.send(Event.LibraryUpdateTriggered(started))
        }
        return started
    }

    /**
     * Update status of episodes.
     *
     * @param download download object containing progress.
     */
    private fun updateDownloadState(download: AnimeDownload) {
        mutableState.update { state ->
            val newItems = state.items.toMutableList().apply {
                val modifiedIndex = indexOfFirst { it.update.episodeId == download.episode.id }
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

    fun downloadEpisodes(items: List<AnimeUpdatesItem>, action: EpisodeDownloadAction) {
        if (items.isEmpty()) return
        coroutineScope.launch {
            when (action) {
                EpisodeDownloadAction.START -> {
                    downloadEpisodes(items)
                    if (items.any { it.downloadStateProvider() == AnimeDownload.State.ERROR }) {
                        AnimeDownloadService.start(Injekt.get<Application>())
                    }
                }
                EpisodeDownloadAction.START_NOW -> {
                    val episodeId = items.singleOrNull()?.update?.episodeId ?: return@launch
                    startDownloadingNow(episodeId)
                }
                EpisodeDownloadAction.START_ALT -> {
                    downloadEpisodes(items, alt = true)
                    if (items.any { it.downloadStateProvider() == AnimeDownload.State.ERROR }) {
                        AnimeDownloadService.start(Injekt.get<Application>())
                    }
                }
                EpisodeDownloadAction.CANCEL -> {
                    val episodeId = items.singleOrNull()?.update?.episodeId ?: return@launch
                    cancelDownload(episodeId)
                }
                EpisodeDownloadAction.DELETE -> {
                    deleteEpisodes(items)
                }
            }
            toggleAllSelection(false)
        }
    }

    private fun startDownloadingNow(episodeId: Long) {
        downloadManager.startDownloadNow(episodeId)
    }

    private fun cancelDownload(episodeId: Long) {
        val activeDownload = downloadManager.getQueuedDownloadOrNull(episodeId) ?: return
        downloadManager.cancelQueuedDownloads(listOf(activeDownload))
        updateDownloadState(activeDownload.apply { status = AnimeDownload.State.NOT_DOWNLOADED })
    }

    /**
     * Mark the selected updates list as seen/unseen.
     * @param updates the list of selected updates.
     * @param seen whether to mark episodes as seen or unseen.
     */
    fun markUpdatesSeen(updates: List<AnimeUpdatesItem>, seen: Boolean) {
        coroutineScope.launchIO {
            setSeenStatus.await(
                seen = seen,
                episodes = updates
                    .mapNotNull { getEpisode.await(it.update.episodeId) }
                    .toTypedArray(),
            )
        }
        toggleAllSelection(false)
    }

    /**
     * Bookmarks the given list of episodes.
     * @param updates the list of episodes to bookmark.
     */
    fun bookmarkUpdates(updates: List<AnimeUpdatesItem>, bookmark: Boolean) {
        coroutineScope.launchIO {
            updates
                .filterNot { it.update.bookmark == bookmark }
                .map { EpisodeUpdate(id = it.update.episodeId, bookmark = bookmark) }
                .let { updateEpisode.awaitAll(it) }
        }
        toggleAllSelection(false)
    }

    /**
     * Downloads the given list of episodes with the manager.
     * @param updatesItem the list of episodes to download.
     */
    private fun downloadEpisodes(updatesItem: List<AnimeUpdatesItem>, alt: Boolean = false) {
        coroutineScope.launchNonCancellable {
            val groupedUpdates = updatesItem.groupBy { it.update.animeId }.values
            for (updates in groupedUpdates) {
                val animeId = updates.first().update.animeId
                val anime = getAnime.await(animeId) ?: continue
                // Don't download if source isn't available
                sourceManager.get(anime.source) ?: continue
                val episodes = updates.mapNotNull { getEpisode.await(it.update.episodeId) }
                downloadManager.downloadEpisodes(anime, episodes, true, alt)
            }
        }
    }

    /**
     * Delete selected episodes
     *
     * @param updatesItem list of episodes
     */
    fun deleteEpisodes(updatesItem: List<AnimeUpdatesItem>) {
        coroutineScope.launchNonCancellable {
            updatesItem
                .groupBy { it.update.animeId }
                .entries
                .forEach { (animeId, updates) ->
                    val anime = getAnime.await(animeId) ?: return@forEach
                    val source = sourceManager.get(anime.source) ?: return@forEach
                    val episodes = updates.mapNotNull { getEpisode.await(it.update.episodeId) }
                    downloadManager.deleteEpisodes(episodes, anime, source)
                }
        }
        toggleAllSelection(false)
    }

    fun showConfirmDeleteEpisodes(updatesItem: List<AnimeUpdatesItem>) {
        setDialog(Dialog.DeleteConfirmation(updatesItem))
    }

    fun toggleSelection(
        item: AnimeUpdatesItem,
        selected: Boolean,
        userSelected: Boolean = false,
        fromLongPress: Boolean = false,
    ) {
        mutableState.update { state ->
            val newItems = state.items.toMutableList().apply {
                val selectedIndex = indexOfFirst { it.update.episodeId == item.update.episodeId }
                if (selectedIndex < 0) return@apply

                val selectedItem = get(selectedIndex)
                if (selectedItem.selected == selected) return@apply

                val firstSelection = none { it.selected }
                set(selectedIndex, selectedItem.copy(selected = selected))
                selectedEpisodeIds.addOrRemove(item.update.episodeId, selected)

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
                                selectedEpisodeIds.add(inbetweenItem.update.episodeId)
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
                selectedEpisodeIds.addOrRemove(it.update.episodeId, selected)
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
                selectedEpisodeIds.addOrRemove(it.update.episodeId, !it.selected)
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
        libraryPreferences.newAnimeUpdatesCount().set(0)
    }

    sealed class Dialog {
        data class DeleteConfirmation(val toDelete: List<AnimeUpdatesItem>) : Dialog()
    }

    sealed class Event {
        object InternalError : Event()
        data class LibraryUpdateTriggered(val started: Boolean) : Event()
    }
}

@Immutable
data class AnimeUpdatesState(
    val isLoading: Boolean = true,
    val items: List<AnimeUpdatesItem> = emptyList(),
    val dialog: AnimeUpdatesScreenModel.Dialog? = null,
) {
    val selected = items.filter { it.selected }
    val selectionMode = selected.isNotEmpty()

    fun getUiModel(context: Context, relativeTime: Int): List<AnimeUpdatesUiModel> {
        val dateFormat = UiPreferences.dateFormat(Injekt.get<UiPreferences>().dateFormat().get())
        return items
            .map { AnimeUpdatesUiModel.Item(it) }
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
                        AnimeUpdatesUiModel.Header(text)
                    }
                    // Return null to avoid adding a separator between two items.
                    else -> null
                }
            }
    }
}

@Immutable
data class AnimeUpdatesItem(
    val update: AnimeUpdatesWithRelations,
    val downloadStateProvider: () -> AnimeDownload.State,
    val downloadProgressProvider: () -> Int,
    val selected: Boolean = false,
)
