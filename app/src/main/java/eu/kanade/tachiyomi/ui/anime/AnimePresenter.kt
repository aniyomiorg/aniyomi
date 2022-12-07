package eu.kanade.tachiyomi.ui.anime

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Immutable
import eu.kanade.core.prefs.CheckboxState
import eu.kanade.core.prefs.mapAsCheckboxState
import eu.kanade.domain.anime.interactor.GetAnimeWithEpisodes
import eu.kanade.domain.anime.interactor.GetDuplicateLibraryAnime
import eu.kanade.domain.anime.interactor.SetAnimeEpisodeFlags
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.animetrack.interactor.DeleteAnimeTrack
import eu.kanade.domain.animetrack.interactor.GetAnimeTracks
import eu.kanade.domain.animetrack.interactor.InsertAnimeTrack
import eu.kanade.domain.animetrack.model.toDbTrack
import eu.kanade.domain.animetrack.model.toDomainTrack
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.category.interactor.GetAnimeCategories
import eu.kanade.domain.category.interactor.SetAnimeCategories
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.download.service.DownloadPreferences
import eu.kanade.domain.episode.interactor.SetAnimeDefaultEpisodeFlags
import eu.kanade.domain.episode.interactor.SetSeenStatus
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.episode.interactor.SyncEpisodesWithTrackServiceTwoWay
import eu.kanade.domain.episode.interactor.UpdateEpisode
import eu.kanade.domain.episode.model.EpisodeUpdate
import eu.kanade.domain.episode.model.applyFilters
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadCache
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadManager
import eu.kanade.tachiyomi.data.animedownload.model.AnimeDownload
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.track.EnhancedTrackService
import eu.kanade.tachiyomi.data.track.MangaTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.anime.track.TrackItem
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.episode.getEpisodeSort
import eu.kanade.tachiyomi.util.episode.getNextUnseen
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import eu.kanade.tachiyomi.util.lang.toRelativeString
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.preference.asHotFlow
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.util.shouldDownloadNewEpisodes
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Date
import java.util.concurrent.TimeUnit
import eu.kanade.domain.anime.model.Anime as DomainAnime
import eu.kanade.domain.episode.model.Episode as DomainEpisode

class AnimePresenter(
    val animeId: Long,
    val isFromSource: Boolean,
    private val basePreferences: BasePreferences = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val downloadManager: AnimeDownloadManager = Injekt.get(),
    private val downloadCache: AnimeDownloadCache = Injekt.get(),
    private val getAnimeAndEpisodes: GetAnimeWithEpisodes = Injekt.get(),
    private val getDuplicateLibraryAnime: GetDuplicateLibraryAnime = Injekt.get(),
    private val setAnimeEpisodeFlags: SetAnimeEpisodeFlags = Injekt.get(),
    private val setAnimeDefaultEpisodeFlags: SetAnimeDefaultEpisodeFlags = Injekt.get(),
    private val setSeenStatus: SetSeenStatus = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get(),
    private val getCategories: GetAnimeCategories = Injekt.get(),
    private val deleteTrack: DeleteAnimeTrack = Injekt.get(),
    private val getTracks: GetAnimeTracks = Injekt.get(),
    private val moveAnimeToCategories: SetAnimeCategories = Injekt.get(),
    private val insertTrack: InsertAnimeTrack = Injekt.get(),
    private val syncEpisodesWithTrackServiceTwoWay: SyncEpisodesWithTrackServiceTwoWay = Injekt.get(),
) : BasePresenter<AnimeController>() {

    private val _state: MutableStateFlow<AnimeScreenState> = MutableStateFlow(AnimeScreenState.Loading)
    val state = _state.asStateFlow()

    private val successState: AnimeScreenState.Success?
        get() = state.value as? AnimeScreenState.Success

    private var _trackList: List<TrackItem> = emptyList()
    val trackList get() = _trackList

    private val loggedServices by lazy { trackManager.services.filter { it.isLogged && it !is MangaTrackService } }

    private var searchTrackerJob: Job? = null
    private var refreshTrackersJob: Job? = null

    val anime: DomainAnime?
        get() = successState?.anime

    val source: AnimeSource?
        get() = successState?.source

    val isFavoritedAnime: Boolean
        get() = anime?.favorite ?: false

    private val processedEpisodes: Sequence<EpisodeItem>?
        get() = successState?.processedEpisodes

    private val selectedPositions: Array<Int> = arrayOf(-1, -1) // first and last selected index in list

    /**
     * Helper function to update the UI state only if it's currently in success state
     */
    private fun updateSuccessState(func: (AnimeScreenState.Success) -> AnimeScreenState.Success) {
        _state.update { if (it is AnimeScreenState.Success) func(it) else it }
    }

    private var incognitoMode = false
        set(value) {
            updateSuccessState { it.copy(isIncognitoMode = value) }
            field = value
        }
    private var downloadedOnlyMode = false
        set(value) {
            updateSuccessState { it.copy(isDownloadedOnlyMode = value) }
            field = value
        }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        val toEpisodeItemsParams: List<DomainEpisode>.(anime: DomainAnime) -> List<EpisodeItem> = { anime ->
            val uiPreferences = Injekt.get<UiPreferences>()
            toEpisodeItems(
                context = view?.activity ?: Injekt.get<Application>(),
                anime = anime,
                dateRelativeTime = uiPreferences.relativeTime().get(),
                dateFormat = UiPreferences.dateFormat(uiPreferences.dateFormat().get()),
            )
        }

        presenterScope.launchIO {
            combine(
                getAnimeAndEpisodes.subscribe(animeId).distinctUntilChanged(),
                downloadCache.changes,
            ) { animeAndEpisodes, _ -> animeAndEpisodes }
                .collectLatest { (anime, episodes) ->
                    val episodeItems = episodes.toEpisodeItemsParams(anime)
                    updateSuccessState {
                        it.copy(
                            anime = anime,
                            episodes = episodeItems,
                        )
                    }
                }
        }

        observeDownloads()

        presenterScope.launchIO {
            val anime = getAnimeAndEpisodes.awaitAnime(animeId)
            val episodes = getAnimeAndEpisodes.awaitEpisodes(animeId)
                .toEpisodeItemsParams(anime)

            if (!anime.favorite) {
                setAnimeDefaultEpisodeFlags.await(anime)
            }

            val needRefreshInfo = !anime.initialized
            val needRefreshEpisode = episodes.isEmpty()

            // Show what we have earlier
            _state.update {
                AnimeScreenState.Success(
                    anime = anime,
                    source = Injekt.get<AnimeSourceManager>().getOrStub(anime.source),
                    isFromSource = isFromSource,
                    trackingAvailable = trackManager.hasLoggedAnimeServices(),
                    episodes = episodes,
                    isRefreshingData = needRefreshInfo || needRefreshEpisode,
                    isIncognitoMode = incognitoMode,
                    isDownloadedOnlyMode = downloadedOnlyMode,
                    dialog = null,
                )
            }
            // Start observe tracking since it only needs mangaId
            observeTrackers()
            observeTrackingCount()

            // Fetch info-episodes when needed
            if (presenterScope.isActive) {
                val fetchFromSourceTasks = listOf(
                    async { if (needRefreshInfo) fetchAnimeFromSource() },
                    async { if (needRefreshEpisode) fetchEpisodesFromSource() },
                )
                fetchFromSourceTasks.awaitAll()
            }

            // Initial loading finished
            updateSuccessState { it.copy(isRefreshingData = false) }
        }

        basePreferences.incognitoMode()
            .asHotFlow { incognitoMode = it }
            .launchIn(presenterScope)

        basePreferences.downloadedOnly()
            .asHotFlow { downloadedOnlyMode = it }
            .launchIn(presenterScope)
    }

    fun fetchAllFromSource(manualFetch: Boolean = true) {
        presenterScope.launch {
            updateSuccessState { it.copy(isRefreshingData = true) }
            val fetchFromSourceTasks = listOf(
                async { fetchAnimeFromSource(manualFetch) },
                async { fetchEpisodesFromSource(manualFetch) },
            )
            fetchFromSourceTasks.awaitAll()
            updateSuccessState { it.copy(isRefreshingData = false) }
        }
    }

    // Anime info - start

    /**
     * Fetch anime information from source.
     */
    private suspend fun fetchAnimeFromSource(manualFetch: Boolean = false) {
        withIOContext {
            try {
                successState?.let {
                    val networkAnime = it.source.getAnimeDetails(it.anime.toSAnime())
                    updateAnime.awaitUpdateFromSource(it.anime, networkAnime, manualFetch)
                }
            } catch (e: Throwable) {
                withUIContext { view?.onFetchAnimeInfoError(e) }
            }
        }
    }

    /**
     * Update favorite status of anime, (removes / adds) anime (to / from) library.
     */
    fun toggleFavorite(
        onRemoved: () -> Unit,
        onAdded: () -> Unit,
        checkDuplicate: Boolean = true,
    ) {
        val state = successState ?: return
        presenterScope.launchIO {
            val anime = state.anime

            if (isFavoritedAnime) {
                // Remove from library
                if (updateAnime.awaitUpdateFavorite(anime.id, false)) {
                    // Remove covers and update last modified in db
                    if (anime.toDbAnime().removeCovers() > 0) {
                        updateAnime.awaitUpdateCoverLastModified(anime.id)
                    }
                    withUIContext { onRemoved() }
                }
            } else {
                // Add to library
                // First, check if duplicate exists if callback is provided
                if (checkDuplicate) {
                    val duplicate = getDuplicateLibraryAnime.await(anime.title, anime.source)
                    if (duplicate != null) {
                        _state.update { state ->
                            when (state) {
                                AnimeScreenState.Loading -> state
                                is AnimeScreenState.Success -> state.copy(dialog = Dialog.DuplicateAnime(anime, duplicate))
                            }
                        }
                        return@launchIO
                    }
                }

                // Now check if user previously set categories, when available
                val categories = getCategories()
                val defaultCategoryId = libraryPreferences.defaultAnimeCategory().get().toLong()
                val defaultCategory = categories.find { it.id == defaultCategoryId }
                when {
                    // Default category set
                    defaultCategory != null -> {
                        val result = updateAnime.awaitUpdateFavorite(anime.id, true)
                        if (!result) return@launchIO
                        moveAnimeToCategory(defaultCategory)
                        withUIContext { onAdded() }
                    }

                    // Automatic 'Default' or no categories
                    defaultCategoryId == 0L || categories.isEmpty() -> {
                        val result = updateAnime.awaitUpdateFavorite(anime.id, true)
                        if (!result) return@launchIO
                        moveAnimeToCategory(null)
                        withUIContext { onAdded() }
                    }

                    // Choose a category
                    else -> promptChangeCategories()
                }

                // Finally match with enhanced tracking when available
                val source = state.source
                trackList
                    .map { it.service }
                    .filterIsInstance<EnhancedTrackService>()
                    .filter { it.accept(source) }
                    .forEach { service ->
                        launchIO {
                            try {
                                service.match(anime.toDbAnime())?.let { track ->
                                    registerTracking(track, service as TrackService)
                                }
                            } catch (e: Exception) {
                                logcat(LogPriority.WARN, e) {
                                    "Could not match anime: ${anime.title} with service $service"
                                }
                            }
                        }
                    }
            }
        }
    }

    fun promptChangeCategories() {
        val state = successState ?: return
        val anime = state.anime
        presenterScope.launch {
            val categories = getCategories()
            val selection = getAnimeCategoryIds(anime)
            _state.update { state ->
                when (state) {
                    AnimeScreenState.Loading -> state
                    is AnimeScreenState.Success -> state.copy(
                        dialog = Dialog.ChangeCategory(
                            anime = anime,
                            initialSelection = categories.mapAsCheckboxState { it.id in selection },
                        ),
                    )
                }
            }
        }
    }

    /**
     * Returns true if the anime has any downloads.
     */
    fun hasDownloads(): Boolean {
        val anime = successState?.anime ?: return false
        return downloadManager.getDownloadCount(anime) > 0
    }

    /**
     * Deletes all the downloads for the anime.
     */
    fun deleteDownloads() {
        val state = successState ?: return
        downloadManager.deleteAnime(state.anime, state.source)
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return getCategories.await().filterNot { it.isSystemCategory }
    }

    /**
     * Gets the category id's the anime is in, if the anime is not in a category, returns the default id.
     *
     * @param anime the anime to get categories from.
     * @return Array of category ids the anime is in, if none returns default id
     */
    private suspend fun getAnimeCategoryIds(anime: DomainAnime): List<Long> {
        return getCategories.await(anime.id)
            .map { it.id }
    }

    fun moveAnimeToCategoriesAndAddToLibrary(anime: DomainAnime, categories: List<Long>) {
        moveAnimeToCategory(categories)
        if (!anime.favorite) {
            presenterScope.launchIO {
                updateAnime.awaitUpdateFavorite(anime.id, true)
            }
        }
    }

    /**
     * Move the given anime to categories.
     *
     * @param categories the selected categories.
     */
    private fun moveAnimeToCategories(categories: List<Category>) {
        val categoryIds = categories.map { it.id }
        moveAnimeToCategory(categoryIds)
    }

    private fun moveAnimeToCategory(categoryIds: List<Long>) {
        presenterScope.launchIO {
            moveAnimeToCategories.await(animeId, categoryIds)
        }
    }

    /**
     * Move the given anime to the category.
     *
     * @param category the selected category, or null for default category.
     */
    private fun moveAnimeToCategory(category: Category?) {
        moveAnimeToCategories(listOfNotNull(category))
    }

    private fun observeTrackingCount() {
        val anime = successState?.anime ?: return

        presenterScope.launchIO {
            getTracks.subscribe(anime.id)
                .catch { logcat(LogPriority.ERROR, it) }
                .map { tracks ->
                    val loggedServicesId = loggedServices.map { it.id }
                    tracks.filter { it.syncId in loggedServicesId }.size
                }
                .collectLatest { trackingCount ->
                    updateSuccessState { it.copy(trackingCount = trackingCount) }
                }
        }
    }

    // Anime info - end

    // Episodes list - start

    private fun observeDownloads() {
        presenterScope.launchIO {
            downloadManager.queue.statusFlow()
                .filter { it.anime.id == successState?.anime?.id }
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .collect {
                    withUIContext {
                        updateDownloadState(it)
                    }
                }
        }

        presenterScope.launchIO {
            downloadManager.queue.progressFlow()
                .filter { it.anime.id == successState?.anime?.id }
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .collect {
                    withUIContext {
                        updateDownloadState(it)
                    }
                }
        }
    }

    private fun updateDownloadState(download: AnimeDownload) {
        updateSuccessState { successState ->
            val modifiedIndex = successState.episodes.indexOfFirst { it.episode.id == download.episode.id }
            if (modifiedIndex < 0) return@updateSuccessState successState

            val newEpisodes = successState.episodes.toMutableList().apply {
                val item = removeAt(modifiedIndex)
                    .copy(downloadState = download.status, downloadProgress = download.progress)
                add(modifiedIndex, item)
            }
            successState.copy(episodes = newEpisodes)
        }
    }

    private fun List<DomainEpisode>.toEpisodeItems(
        context: Context,
        anime: DomainAnime,
        dateRelativeTime: Int,
        dateFormat: DateFormat,
    ): List<EpisodeItem> {
        return map { episode ->
            val activeDownload = downloadManager.queue.find { episode.id == it.episode.id }
            val downloaded = downloadManager.isEpisodeDownloaded(episode.name, episode.scanlator, anime.title, anime.source)
            val downloadState = when {
                activeDownload != null -> activeDownload.status
                downloaded -> AnimeDownload.State.DOWNLOADED
                else -> AnimeDownload.State.NOT_DOWNLOADED
            }
            EpisodeItem(
                episode = episode,
                downloadState = downloadState,
                downloadProgress = activeDownload?.progress ?: 0,
                episodeTitleString = if (anime.displayMode == DomainAnime.EPISODE_DISPLAY_NUMBER) {
                    context.getString(
                        R.string.display_mode_episode,
                        episodeDecimalFormat.format(episode.episodeNumber.toDouble()),
                    )
                } else {
                    episode.name
                },
                dateUploadString = episode.dateUpload
                    .takeIf { it > 0 }
                    ?.let {
                        Date(it).toRelativeString(
                            context,
                            dateRelativeTime,
                            dateFormat,
                        )
                    },
                seenProgressString = episode.lastSecondSeen.takeIf { !episode.seen && it > 0 }?.let {
                    context.getString(
                        R.string.episode_progress,
                        formatProgress(it),
                        formatProgress(episode.totalSeconds),
                    )
                },
            )
        }
    }

    /**
     * Requests an updated list of episodes from the source.
     */
    private suspend fun fetchEpisodesFromSource(manualFetch: Boolean = false) {
        withIOContext {
            try {
                successState?.let { successState ->
                    val episodes = successState.source.getEpisodeList(successState.anime.toSAnime())

                    val newEpisodes = syncEpisodesWithSource.await(
                        episodes,
                        successState.anime,
                        successState.source,
                    )

                    if (manualFetch) {
                        downloadNewEpisodes(newEpisodes)
                    }
                }
            } catch (e: Throwable) {
                withUIContext { view?.onFetchEpisodesError(e) }
            }
        }
    }

    /**
     * Returns the next unseen episode or null if everything is seen.
     */
    fun getNextUnseenEpisode(): DomainEpisode? {
        val successState = successState ?: return null
        return successState.episodes.getNextUnseen(successState.anime)
    }

    fun getUnseenEpisodes(): List<DomainEpisode> {
        return successState?.processedEpisodes
            ?.filter { (episode, dlStatus) -> !episode.seen && dlStatus == AnimeDownload.State.NOT_DOWNLOADED }
            ?.map { it.episode }
            ?.toList()
            ?: emptyList()
    }

    fun getUnseenEpisodesSorted(): List<DomainEpisode> {
        val anime = successState?.anime ?: return emptyList()
        val episodes = getUnseenEpisodes().sortedWith(getEpisodeSort(anime))
        return if (anime.sortDescending()) episodes.reversed() else episodes
    }

    fun startDownloadingNow(episodeId: Long) {
        downloadManager.startDownloadNow(episodeId)
    }

    fun cancelDownload(episodeId: Long) {
        val activeDownload = downloadManager.queue.find { episodeId == it.episode.id } ?: return
        downloadManager.deletePendingDownload(activeDownload)
        updateDownloadState(activeDownload.apply { status = AnimeDownload.State.NOT_DOWNLOADED })
    }

    fun markPreviousEpisodeSeen(pointer: DomainEpisode) {
        val successState = successState ?: return
        val episodes = processedEpisodes.orEmpty().map { it.episode }.toList()
        val prevEpisodes = if (successState.anime.sortDescending()) episodes.asReversed() else episodes
        val pointerPos = prevEpisodes.indexOf(pointer)
        if (pointerPos != -1) markEpisodesSeen(prevEpisodes.take(pointerPos), true)
    }

    /**
     * Mark the selected episode list as seen/unseen.
     * @param episodes the list of selected episodes.
     * @param seen whether to mark episodes as seen or unseen.
     */
    fun markEpisodesSeen(episodes: List<DomainEpisode>, seen: Boolean) {
        presenterScope.launchIO {
            setSeenStatus.await(
                seen = seen,
                episodes = episodes.toTypedArray(),
            )
        }
        toggleAllSelection(false)
    }

    /**
     * Downloads the given list of episodes with the manager.
     * @param episodes the list of episodes to download.
     */
    fun downloadEpisodes(episodes: List<DomainEpisode>, alt: Boolean = false) {
        val anime = successState?.anime ?: return
        if (alt) {
            downloadManager.downloadEpisodesAlt(anime, episodes.map { it.toDbEpisode() })
        } else {
            downloadManager.downloadEpisodes(anime, episodes.map { it.toDbEpisode() })
        }
        toggleAllSelection(false)
    }

    /**
     * Bookmarks the given list of episodes.
     * @param episodes the list of episodes to bookmark.
     */
    fun bookmarkEpisodes(episodes: List<DomainEpisode>, bookmarked: Boolean) {
        presenterScope.launchIO {
            episodes
                .filterNot { it.bookmark == bookmarked }
                .map { EpisodeUpdate(id = it.id, bookmark = bookmarked) }
                .let { updateEpisode.awaitAll(it) }
        }
        toggleAllSelection(false)
    }

    /**
     * Deletes the given list of episode.
     *
     * @param episodes the list of episodes to delete.
     */
    fun deleteEpisodes(episodes: List<DomainEpisode>) {
        presenterScope.launchNonCancellable {
            try {
                successState?.let { state ->
                    downloadManager.deleteEpisodes(
                        episodes.map { it.toDbEpisode() },
                        state.anime,
                        state.source,
                    )
                }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
            }
        }
    }

    private fun downloadNewEpisodes(episodes: List<DomainEpisode>) {
        presenterScope.launchNonCancellable {
            val anime = successState?.anime ?: return@launchNonCancellable
            val categories = getCategories.await(anime.id).map { it.id }
            if (episodes.isEmpty() || !anime.shouldDownloadNewEpisodes(categories, downloadPreferences)) return@launchNonCancellable
            downloadEpisodes(episodes)
        }
    }

    /**
     * Sets the seen filter and requests an UI update.
     * @param state whether to display only unseen episodes or all episodes.
     */
    fun setUnseenFilter(state: State) {
        val anime = successState?.anime ?: return

        val flag = when (state) {
            State.IGNORE -> DomainAnime.SHOW_ALL
            State.INCLUDE -> DomainAnime.EPISODE_SHOW_UNSEEN
            State.EXCLUDE -> DomainAnime.EPISODE_SHOW_SEEN
        }
        presenterScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetUnseenFilter(anime, flag)
        }
    }

    /**
     * Sets the download filter and requests an UI update.
     * @param state whether to display only downloaded episodes or all episodes.
     */
    fun setDownloadedFilter(state: State) {
        val anime = successState?.anime ?: return

        val flag = when (state) {
            State.IGNORE -> DomainAnime.SHOW_ALL
            State.INCLUDE -> DomainAnime.EPISODE_SHOW_DOWNLOADED
            State.EXCLUDE -> DomainAnime.EPISODE_SHOW_NOT_DOWNLOADED
        }

        presenterScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetDownloadedFilter(anime, flag)
        }
    }

    /**
     * Sets the bookmark filter and requests an UI update.
     * @param state whether to display only bookmarked episodes or all episodes.
     */
    fun setBookmarkedFilter(state: State) {
        val anime = successState?.anime ?: return

        val flag = when (state) {
            State.IGNORE -> DomainAnime.SHOW_ALL
            State.INCLUDE -> DomainAnime.EPISODE_SHOW_BOOKMARKED
            State.EXCLUDE -> DomainAnime.EPISODE_SHOW_NOT_BOOKMARKED
        }

        presenterScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetBookmarkFilter(anime, flag)
        }
    }

    /**
     * Sets the active display mode.
     * @param mode the mode to set.
     */
    fun setDisplayMode(mode: Long) {
        val anime = successState?.anime ?: return

        presenterScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetDisplayMode(anime, mode)
        }
    }

    /**
     * Sets the sorting method and requests an UI update.
     * @param sort the sorting mode.
     */
    fun setSorting(sort: Long) {
        val anime = successState?.anime ?: return

        presenterScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetSortingModeOrFlipOrder(anime, sort)
        }
    }

    fun toggleSelection(
        item: EpisodeItem,
        selected: Boolean,
        userSelected: Boolean = false,
        fromLongPress: Boolean = false,
    ) {
        updateSuccessState { successState ->
            val newEpisodes = successState.processedEpisodes.toMutableList().apply {
                val modifiedIndex = successState.processedEpisodes.indexOfFirst { it == item }
                if (modifiedIndex < 0) return@apply

                val oldItem = get(modifiedIndex)
                if ((oldItem.selected && selected) || (!oldItem.selected && !selected)) return@apply

                val firstSelection = none { it.selected }
                var newItem = removeAt(modifiedIndex)
                add(modifiedIndex, newItem.copy(selected = selected))

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
                            newItem = removeAt(it)
                            add(it, newItem.copy(selected = true))
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
            successState.copy(episodes = newEpisodes)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        updateSuccessState { successState ->
            val newEpisodes = successState.episodes.map {
                it.copy(selected = selected)
            }
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            successState.copy(episodes = newEpisodes)
        }
    }

    fun invertSelection() {
        updateSuccessState { successState ->
            val newEpisodes = successState.episodes.map {
                it.copy(selected = !it.selected)
            }
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            successState.copy(episodes = newEpisodes)
        }
    }

    // Episodes list - end

    // Track sheet - start

    private fun observeTrackers() {
        val anime = successState?.anime ?: return

        presenterScope.launchIO {
            getTracks.subscribe(anime.id)
                .catch { logcat(LogPriority.ERROR, it) }
                .map { tracks ->
                    val dbTracks = tracks.map { it.toDbTrack() }
                    loggedServices
                        // Map to TrackItem
                        .map { service -> TrackItem(dbTracks.find { it.sync_id.toLong() == service.id }, service) }
                        // Show only if the service supports this manga's source
                        .filter { (it.service as? EnhancedTrackService)?.accept(source!!) ?: true }
                }
                .collectLatest { trackItems ->
                    _trackList = trackItems
                    withContext(Dispatchers.Main) {
                        view?.onNextTrackers(trackItems)
                    }
                }
        }
    }

    fun refreshTrackers() {
        refreshTrackersJob?.cancel()
        refreshTrackersJob = presenterScope.launchNonCancellable {
            supervisorScope {
                try {
                    trackList
                        .map {
                            async {
                                val track = it.track ?: return@async null

                                val updatedTrack = it.service.refresh(track)

                                val domainTrack = updatedTrack.toDomainTrack() ?: return@async null
                                insertTrack.await(domainTrack)

                                (it.service as? EnhancedTrackService)?.let { _ ->
                                    val allEpisodes = successState?.episodes
                                        ?.map { it.episode } ?: emptyList()

                                    syncEpisodesWithTrackServiceTwoWay
                                        .await(allEpisodes, domainTrack, it.service)
                                }
                            }
                        }
                        .awaitAll()

                    withUIContext { view?.onTrackingRefreshDone() }
                } catch (e: Throwable) {
                    withUIContext { view?.onTrackingRefreshError(e) }
                }
            }
        }
    }

    fun trackingSearch(query: String, service: TrackService) {
        searchTrackerJob?.cancel()
        searchTrackerJob = presenterScope.launchIO {
            try {
                val results = service.searchAnime(query)
                withUIContext { view?.onTrackingSearchResults(results) }
            } catch (e: Throwable) {
                withUIContext { view?.onTrackingSearchResultsError(e) }
            }
        }
    }

    fun registerTracking(item: AnimeTrack?, service: TrackService) {
        val successState = successState ?: return
        if (item != null) {
            item.anime_id = successState.anime.id
            presenterScope.launchNonCancellable {
                try {
                    val allEpisodes = successState.episodes.map { it.episode }
                    val hasSeenEpisodes = allEpisodes.any { it.seen }
                    service.bind(item, hasSeenEpisodes)

                    item.toDomainTrack(idRequired = false)?.let { track ->
                        insertTrack.await(track)

                        // Update episode progress if newer episodes marked read locally
                        if (hasSeenEpisodes) {
                            val latestLocalSeenEpisodeNumber = allEpisodes
                                .sortedBy { it.episodeNumber }
                                .takeWhile { it.seen }
                                .lastOrNull()
                                ?.episodeNumber?.toDouble() ?: -1.0

                            if (latestLocalSeenEpisodeNumber > track.lastEpisodeSeen) {
                                val updatedTrack = track.copy(
                                    lastEpisodeSeen = latestLocalSeenEpisodeNumber,
                                )
                                setTrackerLastEpisodeSeen(TrackItem(updatedTrack.toDbTrack(), service), latestLocalSeenEpisodeNumber.toInt())
                            }
                        }
                        if (service is EnhancedTrackService) {
                            syncEpisodesWithTrackServiceTwoWay.await(allEpisodes, track, service)
                        }
                    }
                } catch (e: Throwable) {
                    withUIContext { view?.applicationContext?.toast(e.message) }
                }
            }
        } else {
            unregisterTracking(service)
        }
    }

    fun unregisterTracking(service: TrackService) {
        val anime = successState?.anime ?: return

        presenterScope.launchNonCancellable {
            deleteTrack.await(anime.id, service.id)
        }
    }

    private fun updateRemote(track: AnimeTrack, service: TrackService) {
        presenterScope.launchNonCancellable {
            try {
                service.update(track)

                track.toDomainTrack(idRequired = false)?.let {
                    insertTrack.await(it)
                }

                withUIContext { view?.onTrackingRefreshDone() }
            } catch (e: Throwable) {
                withUIContext { view?.onTrackingRefreshError(e) }

                // Restart on error to set old values
                observeTrackers()
            }
        }
    }

    fun setTrackerStatus(item: TrackItem, index: Int) {
        val track = item.track!!
        track.status = item.service.getStatusListAnime()[index]
        if (track.status == item.service.getCompletionStatus() && track.total_episodes != 0) {
            track.last_episode_seen = track.total_episodes.toFloat()
        }
        updateRemote(track, item.service)
    }

    fun setTrackerScore(item: TrackItem, index: Int) {
        val track = item.track!!
        track.score = item.service.indexToScore(index)
        updateRemote(track, item.service)
    }

    fun setTrackerLastEpisodeSeen(item: TrackItem, episodeNumber: Int) {
        val track = item.track!!
        if (track.last_episode_seen == 0F && track.last_episode_seen < episodeNumber && track.status != item.service.getRewatchingStatus()) {
            track.status = item.service.getWatchingStatus()
        }
        track.last_episode_seen = episodeNumber.toFloat()
        if (track.total_episodes != 0 && track.last_episode_seen.toInt() == track.total_episodes) {
            track.status = item.service.getCompletionStatus()
        }
        updateRemote(track, item.service)
    }

    fun setTrackerStartDate(item: TrackItem, date: Long) {
        val track = item.track!!
        track.started_watching_date = date
        updateRemote(track, item.service)
    }

    fun setTrackerFinishDate(item: TrackItem, date: Long) {
        val track = item.track!!
        track.finished_watching_date = date
        updateRemote(track, item.service)
    }

    // Track sheet - end

    fun getAnimeSourceOrStub(anime: DomainAnime): AnimeSource {
        return sourceManager.getOrStub(anime.source)
    }

    sealed class Dialog {
        data class ChangeCategory(val anime: DomainAnime, val initialSelection: List<CheckboxState<Category>>) : Dialog()
        data class DeleteEpisodes(val episodes: List<DomainEpisode>) : Dialog()
        data class DuplicateAnime(val anime: DomainAnime, val duplicate: DomainAnime) : Dialog()
        data class DownloadCustomAmount(val max: Int) : Dialog()
    }

    fun dismissDialog() {
        _state.update { state ->
            when (state) {
                AnimeScreenState.Loading -> state
                is AnimeScreenState.Success -> state.copy(dialog = null)
            }
        }
    }

    fun showDownloadCustomDialog() {
        val max = processedEpisodes?.count() ?: return
        _state.update { state ->
            when (state) {
                AnimeScreenState.Loading -> state
                is AnimeScreenState.Success -> state.copy(dialog = Dialog.DownloadCustomAmount(max))
            }
        }
    }

    fun showDeleteEpisodeDialog(chapters: List<DomainEpisode>) {
        _state.update { state ->
            when (state) {
                AnimeScreenState.Loading -> state
                is AnimeScreenState.Success -> state.copy(dialog = Dialog.DeleteEpisodes(chapters))
            }
        }
    }
}

sealed class AnimeScreenState {
    @Immutable
    object Loading : AnimeScreenState()

    @Immutable
    data class Success(
        val anime: DomainAnime,
        val source: AnimeSource,
        val isFromSource: Boolean,
        val episodes: List<EpisodeItem>,
        val trackingAvailable: Boolean = false,
        val trackingCount: Int = 0,
        val isRefreshingData: Boolean = false,
        val isIncognitoMode: Boolean = false,
        val isDownloadedOnlyMode: Boolean = false,
        val dialog: AnimePresenter.Dialog? = null,
    ) : AnimeScreenState() {

        val processedEpisodes: Sequence<EpisodeItem>
            get() = episodes.applyFilters(anime)
    }
}

@Immutable
data class EpisodeItem(
    val episode: DomainEpisode,
    val downloadState: AnimeDownload.State,
    val downloadProgress: Int,

    val episodeTitleString: String,
    val dateUploadString: String?,
    val seenProgressString: String?,

    val selected: Boolean = false,
) {
    val isDownloaded = downloadState == AnimeDownload.State.DOWNLOADED
}

private val episodeDecimalFormat = DecimalFormat(
    "#.###",
    DecimalFormatSymbols()
        .apply { decimalSeparator = '.' },
)

private fun formatProgress(milliseconds: Long): String {
    return if (milliseconds > 3600000L) {
        String.format(
            "%d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(milliseconds),
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    } else {
        String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    }
}
