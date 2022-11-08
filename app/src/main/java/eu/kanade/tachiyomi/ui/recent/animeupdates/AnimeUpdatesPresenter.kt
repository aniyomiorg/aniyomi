package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat.startActivityForResult
import eu.kanade.domain.anime.interactor.GetAnime
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.animeupdates.interactor.GetAnimeUpdates
import eu.kanade.domain.animeupdates.model.AnimeUpdatesWithRelations
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.episode.interactor.GetEpisode
import eu.kanade.domain.episode.interactor.SetSeenStatus
import eu.kanade.domain.episode.interactor.UpdateEpisode
import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.model.EpisodeUpdate
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.animeupdates.AnimeUpdatesState
import eu.kanade.presentation.animeupdates.AnimeUpdatesStateImpl
import eu.kanade.presentation.components.EpisodeDownloadAction
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadCache
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadManager
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadService
import eu.kanade.tachiyomi.data.animedownload.model.AnimeDownload
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.player.EpisodeLoader
import eu.kanade.tachiyomi.ui.player.ExternalIntents
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.setting.PlayerPreferences
import eu.kanade.tachiyomi.util.lang.awaitSingle
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class AnimeUpdatesPresenter(
    private val presenterScope: CoroutineScope,
    private val state: AnimeUpdatesStateImpl = AnimeUpdatesState() as AnimeUpdatesStateImpl,
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val setSeenStatus: SetSeenStatus = Injekt.get(),
    private val getUpdates: GetAnimeUpdates = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val downloadManager: AnimeDownloadManager = Injekt.get(),
    private val downloadCache: AnimeDownloadCache = Injekt.get(),
    private val getEpisode: GetEpisode = Injekt.get(),
    basePreferences: BasePreferences = Injekt.get(),
    uiPreferences: UiPreferences = Injekt.get(),
    libraryPreferences: LibraryPreferences = Injekt.get(),
    playerPreferences: PlayerPreferences = Injekt.get(),
) : AnimeUpdatesState by state {

    val isDownloadOnly = basePreferences.downloadedOnly().get()
    val isIncognitoMode = basePreferences.incognitoMode().get()

    var activity: Activity? = null
    var context: Context? = null

    val lastUpdated = libraryPreferences.libraryUpdateLastTimestamp().get()

    val useExternalPlayer = playerPreferences.alwaysUseExternalPlayer().get()

    val relativeTime = uiPreferences.relativeTime().get()
    val dateFormat: DateFormat by mutableStateOf(UiPreferences.dateFormat(uiPreferences.dateFormat().get()))

    private val _events: Channel<Event> = Channel(Int.MAX_VALUE)
    val events: Flow<Event> = _events.receiveAsFlow()

    // First and last selected index in list
    private val selectedPositions: Array<Int> = arrayOf(-1, -1)

    fun onCreate(activity: Activity?) {
        this.activity = activity
        this.context = activity

        presenterScope.launchIO {
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
                .stateIn(presenterScope)
                .collectLatest { updates ->
                    state.items = updates.toAnimeUpdateItems()
                    state.isLoading = false
                }
        }

        presenterScope.launchIO {
            downloadManager.queue.statusFlow()
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .collect {
                    withUIContext {
                        updateDownloadState(it)
                    }
                }
        }

        presenterScope.launchIO {
            downloadManager.queue.progressFlow()
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .collect {
                    withUIContext {
                        updateDownloadState(it)
                    }
                }
        }
    }
    private fun List<AnimeUpdatesWithRelations>.toAnimeUpdateItems(): List<AnimeUpdatesItem> {
        return this
            .distinctBy { it.episodeId }
            .map {
                val activeDownload = downloadManager.queue.find { download -> it.episodeId == download.episode.id }
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
                )
            }
    }

    /**
     * Update status of episodes.
     *
     * @param download download object containing progress.
     */
    private fun updateDownloadState(download: AnimeDownload) {
        state.items = items.toMutableList().apply {
            val modifiedIndex = indexOfFirst {
                it.update.episodeId == download.episode.id
            }
            if (modifiedIndex < 0) return@apply

            val item = removeAt(modifiedIndex)
                .copy(
                    downloadStateProvider = { download.status },
                    downloadProgressProvider = { download.progress },
                )
            add(modifiedIndex, item)
        }
    }

    fun downloadEpisodes(items: List<AnimeUpdatesItem>, action: EpisodeDownloadAction) {
        if (items.isEmpty()) return
        presenterScope.launch {
            when (action) {
                EpisodeDownloadAction.START -> {
                    downloadEpisodes(items)
                    if (items.any { it.downloadStateProvider() == AnimeDownload.State.ERROR }) {
                        AnimeDownloadService.start(context!!)
                    }
                }
                EpisodeDownloadAction.START_NOW -> {
                    val episodeId = items.singleOrNull()?.update?.episodeId ?: return@launch
                    startDownloadingNow(episodeId)
                }
                EpisodeDownloadAction.CANCEL -> {
                    val episodeId = items.singleOrNull()?.update?.episodeId ?: return@launch
                    cancelDownload(episodeId)
                }
                EpisodeDownloadAction.DELETE -> {
                    deleteEpisodes(items)
                }
                EpisodeDownloadAction.START_ALT -> {
                    downloadEpisodesAlternatively(items)
                    if (items.any { it.downloadStateProvider() == AnimeDownload.State.ERROR }) {
                        AnimeDownloadService.start(context!!)
                    }
                }
            }
            toggleAllSelection(false)
        }
    }

    private fun startDownloadingNow(episodeId: Long) {
        downloadManager.startDownloadNow(episodeId)
    }

    private fun cancelDownload(episodeId: Long) {
        val activeDownload = downloadManager.queue.find { episodeId == it.episode.id } ?: return
        downloadManager.deletePendingDownload(activeDownload)
        updateDownloadState(activeDownload.apply { status = AnimeDownload.State.NOT_DOWNLOADED })
    }

    /**
     * Mark the selected updates list as seen/unseen.
     * @param updates the list of selected updates.
     * @param seen whether to mark chapters seen or unseen.
     */
    fun markUpdatesSeen(updates: List<AnimeUpdatesItem>, seen: Boolean) {
        presenterScope.launchIO {
            setSeenStatus.await(
                seen = seen,
                episodes = updates
                    .mapNotNull { getEpisode.await(it.update.episodeId) }
                    .toTypedArray(),
            )
        }
    }

    /**
     * Bookmarks the given list of episodes.
     * @param updates the list of episodes to bookmark.
     */
    fun bookmarkUpdates(updates: List<AnimeUpdatesItem>, bookmark: Boolean) {
        presenterScope.launchIO {
            updates
                .filterNot { it.update.bookmark == bookmark }
                .map { EpisodeUpdate(id = it.update.episodeId, bookmark = bookmark) }
                .let { updateEpisode.awaitAll(it) }
        }
    }

    /**
     * *Downloads the given list of episodes with the manager.
     * @param updatesItem the list of episodes to download.
     */
    fun downloadEpisodes(updatesItem: List<AnimeUpdatesItem>) {
        presenterScope.launchNonCancellable {
            val groupedUpdates = updatesItem.groupBy { it.update.animeId }.values
            for (updates in groupedUpdates) {
                val animeId = updates.first().update.animeId
                val anime = getAnime.await(animeId) ?: continue
                // Don't download if source isn't available
                sourceManager.get(anime.source) ?: continue
                val episodes = updates.mapNotNull { getEpisode.await(it.update.episodeId)?.toDbEpisode() }
                downloadManager.downloadEpisodes(anime, episodes)
            }
        }
    }

    /**
     * Download selected episodes alternatively
     * @param updatesItem the list of episodes to download.
     */
    private fun downloadEpisodesAlternatively(updatesItem: List<AnimeUpdatesItem>) {
        presenterScope.launchNonCancellable {
            val groupedUpdates = updatesItem.groupBy { it.update.animeId }.values
            for (updates in groupedUpdates) {
                val animeId = updates.first().update.animeId
                val anime = getAnime.await(animeId) ?: continue
                // Don't download if source isn't available
                sourceManager.get(anime.source) ?: continue
                val episodes = updates.mapNotNull { getEpisode.await(it.update.episodeId)?.toDbEpisode() }
                downloadManager.downloadEpisodesAlt(anime, episodes)
            }
        }
    }

    /**
     * Delete selected episodes
     *
     * @param updatesItem list of episodes
     */
    fun deleteEpisodes(updatesItem: List<AnimeUpdatesItem>) {
        presenterScope.launchNonCancellable {
            updatesItem
                .groupBy { it.update.animeId }
                .entries
                .forEach { (animeId, updates) ->
                    val anime = getAnime.await(animeId) ?: return@forEach
                    val source = sourceManager.get(anime.source) ?: return@forEach
                    val episodes = updates.mapNotNull { getEpisode.await(it.update.episodeId)?.toDbEpisode() }
                    downloadManager.deleteEpisodes(episodes, anime, source)
                }
        }
    }

    fun openEpisode(updatesItem: List<AnimeUpdatesItem>, altPlayer: Boolean = false) {
        presenterScope.launchNonCancellable {
            updatesItem
                .groupBy { it.update.animeId }
                .entries
                .forEach { (animeId, updates) ->
                    val anime = getAnime.await(animeId) ?: return@forEach
                    val source = sourceManager.get(anime.source) ?: return@forEach
                    val episode = updates.firstNotNullOf { getEpisode.await(it.update.episodeId) }

                    if (useExternalPlayer != altPlayer) {
                        openEpisodeExternal(episode, anime, source)
                    } else {
                        openEpisodeInternal(episode)
                    }
                }
        }
    }

    private fun openEpisodeInternal(episode: Episode) {
        val context = context ?: return
        context.startActivity(PlayerActivity.newIntent(context, episode.animeId, episode.id))
    }

    private fun openEpisodeExternal(episode: Episode, anime: Anime, source: AnimeSource) {
        val context = context ?: return
        val activity = activity ?: return
        launchIO {
            val video = try {
                EpisodeLoader.getLink(episode.toDbEpisode(), anime.toDbAnime(), source)
                    .awaitSingle()
            } catch (e: Exception) {
                launchUI { context.toast(e.message) }
                return@launchIO
            }
            if (video != null) {
                AnimeController.EXT_EPISODE = episode
                AnimeController.EXT_ANIME = anime

                val extIntent = ExternalIntents(anime, source).getExternalIntent(
                    episode,
                    video,
                    context,
                )
                if (extIntent != null) {
                    try {
                        startActivityForResult(activity, extIntent, AnimeController.REQUEST_EXTERNAL, null)
                    } catch (e: Exception) {
                        launchUI { context.toast(e.message) }
                        return@launchIO
                    }
                }
            } else {
                launchUI { context.toast("Couldn't find any video links.") }
                return@launchIO
            }
        }
    }

    fun toggleSelection(
        item: AnimeUpdatesItem,
        selected: Boolean,
        userSelected: Boolean = false,
        fromLongPress: Boolean = false,
    ) {
        state.items = items.toMutableList().apply {
            val modifiedIndex = indexOfFirst { it == item }
            if (modifiedIndex < 0) return@apply

            val oldItem = get(modifiedIndex)
            if (oldItem.selected == selected) return@apply

            val firstSelection = none { it.selected }
            var newItem = removeAt(modifiedIndex).copy(selected = selected)
            add(modifiedIndex, newItem)

            if (selected && userSelected && fromLongPress) {
                if (firstSelection) {
                    selectedPositions[0] = modifiedIndex
                    selectedPositions[1] = modifiedIndex
                } else {
                    // Try to select the items in-between when possible
                    val range: IntRange
                    if (modifiedIndex < selectedPositions[0]) {
                        range = modifiedIndex + 1 until selectedPositions[0]
                        selectedPositions[0] = modifiedIndex
                    } else if (modifiedIndex > selectedPositions[1]) {
                        range = (selectedPositions[1] + 1) until modifiedIndex
                        selectedPositions[1] = modifiedIndex
                    } else {
                        // Just select itself
                        range = IntRange.EMPTY
                    }

                    range.forEach {
                        newItem = removeAt(it).copy(selected = true)
                        add(it, newItem)
                    }
                }
            } else if (userSelected && !fromLongPress) {
                if (!selected) {
                    if (modifiedIndex == selectedPositions[0]) {
                        selectedPositions[0] = indexOfFirst { it.selected }
                    } else if (modifiedIndex == selectedPositions[1]) {
                        selectedPositions[1] = indexOfLast { it.selected }
                    }
                } else {
                    if (modifiedIndex < selectedPositions[0]) {
                        selectedPositions[0] = modifiedIndex
                    } else if (modifiedIndex > selectedPositions[1]) {
                        selectedPositions[1] = modifiedIndex
                    }
                }
            }
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        state.items = items.map {
            it.copy(selected = selected)
        }
        selectedPositions[0] = -1
        selectedPositions[1] = -1
    }

    fun invertSelection() {
        state.items = items.map {
            it.copy(selected = !it.selected)
        }
        selectedPositions[0] = -1
        selectedPositions[1] = -1
    }

    sealed class Dialog {
        data class DeleteConfirmation(val toDelete: List<AnimeUpdatesItem>) : Dialog()
    }

    sealed class Event {
        object InternalError : Event()
    }
}

@Immutable
data class AnimeUpdatesItem(
    val update: AnimeUpdatesWithRelations,
    val downloadStateProvider: () -> AnimeDownload.State,
    val downloadProgressProvider: () -> Int,
    val selected: Boolean = false,
)
