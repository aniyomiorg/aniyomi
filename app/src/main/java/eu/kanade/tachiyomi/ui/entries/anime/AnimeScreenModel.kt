package eu.kanade.tachiyomi.ui.entries.anime

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.prefs.CheckboxState
import eu.kanade.core.prefs.mapAsCheckboxState
import eu.kanade.core.util.addOrRemove
import eu.kanade.data.items.episode.NoEpisodesException
import eu.kanade.domain.category.anime.interactor.GetAnimeCategories
import eu.kanade.domain.category.anime.interactor.SetAnimeCategories
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.download.service.DownloadPreferences
import eu.kanade.domain.entries.TriStateFilter
import eu.kanade.domain.entries.anime.interactor.GetAnimeWithEpisodes
import eu.kanade.domain.entries.anime.interactor.GetDuplicateLibraryAnime
import eu.kanade.domain.entries.anime.interactor.SetAnimeEpisodeFlags
import eu.kanade.domain.entries.anime.interactor.UpdateAnime
import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.domain.entries.anime.model.isLocal
import eu.kanade.domain.items.episode.interactor.SetAnimeDefaultEpisodeFlags
import eu.kanade.domain.items.episode.interactor.SetSeenStatus
import eu.kanade.domain.items.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.items.episode.interactor.UpdateEpisode
import eu.kanade.domain.items.episode.model.Episode
import eu.kanade.domain.items.episode.model.EpisodeUpdate
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.domain.track.anime.interactor.GetAnimeTracks
import eu.kanade.domain.track.anime.model.toDbTrack
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.EpisodeDownloadAction
import eu.kanade.presentation.entries.DownloadAction
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadCache
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadService
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.data.track.AnimeTrackService
import eu.kanade.tachiyomi.data.track.EnhancedAnimeTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import eu.kanade.tachiyomi.ui.entries.anime.track.AnimeTrackItem
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.util.episode.getEpisodeSort
import eu.kanade.tachiyomi.util.episode.getNextUnseen
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import eu.kanade.tachiyomi.util.lang.toRelativeString
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.util.shouldDownloadNewEpisodes
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Date
import java.util.concurrent.TimeUnit

class AnimeInfoScreenModel(
    val context: Context,
    val animeId: Long,
    private val isFromSource: Boolean,
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val uiPreferences: UiPreferences = Injekt.get(),
    private val trackPreferences: TrackPreferences = Injekt.get(),
    internal val playerPreferences: PlayerPreferences = Injekt.get(),
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
    private val getTracks: GetAnimeTracks = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : StateScreenModel<AnimeScreenState>(AnimeScreenState.Loading) {

    private val successState: AnimeScreenState.Success?
        get() = state.value as? AnimeScreenState.Success

    private val loggedServices by lazy { trackManager.services.filter { it.isLogged && it is AnimeTrackService } }

    val anime: Anime?
        get() = successState?.anime

    val source: AnimeSource?
        get() = successState?.source

    private val isFavoritedAnime: Boolean
        get() = anime?.favorite ?: false

    private val processedEpisodes: Sequence<EpisodeItem>?
        get() = successState?.processedEpisodes

    private val selectedPositions: Array<Int> = arrayOf(-1, -1) // first and last selected index in list
    private val selectedEpisodeIds: HashSet<Long> = HashSet()

    internal var isFromChangeCategory: Boolean = false

    internal val autoOpenTrack: Boolean
        get() = successState?.trackingAvailable == true && trackPreferences.trackOnAddingToLibrary().get()

    /**
     * Helper function to update the UI state only if it's currently in success state
     */
    private fun updateSuccessState(func: (AnimeScreenState.Success) -> AnimeScreenState.Success) {
        mutableState.update { if (it is AnimeScreenState.Success) func(it) else it }
    }

    init {
        val toEpisodeItemsParams: List<Episode>.(anime: Anime) -> List<EpisodeItem> = { anime ->
            toEpisodeItems(
                context = context,
                anime = anime,
                dateRelativeTime = uiPreferences.relativeTime().get(),
                dateFormat = UiPreferences.dateFormat(uiPreferences.dateFormat().get()),
            )
        }

        coroutineScope.launchIO {
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

        coroutineScope.launchIO {
            val anime = getAnimeAndEpisodes.awaitAnime(animeId)
            val episodes = getAnimeAndEpisodes.awaitEpisodes(animeId)
                .toEpisodeItemsParams(anime)

            if (!anime.favorite) {
                setAnimeDefaultEpisodeFlags.await(anime)
            }

            val needRefreshInfo = !anime.initialized
            val needRefreshEpisode = episodes.isEmpty()

            // Show what we have earlier
            mutableState.update {
                AnimeScreenState.Success(
                    anime = anime,
                    source = Injekt.get<AnimeSourceManager>().getOrStub(anime.source),
                    isFromSource = isFromSource,
                    episodes = episodes,
                    isRefreshingData = needRefreshInfo || needRefreshEpisode,
                    dialog = null,
                )
            }
            // Start observe tracking since it only needs mangaId
            observeTrackers()

            // Fetch info-episodes when needed
            if (coroutineScope.isActive) {
                val fetchFromSourceTasks = listOf(
                    async { if (needRefreshInfo) fetchAnimeFromSource() },
                    async { if (needRefreshEpisode) fetchEpisodesFromSource() },
                )
                fetchFromSourceTasks.awaitAll()
            }

            // Initial loading finished
            updateSuccessState { it.copy(isRefreshingData = false) }
        }
    }

    fun fetchAllFromSource(manualFetch: Boolean = true) {
        coroutineScope.launch {
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
        val state = successState ?: return
        try {
            withIOContext {
                val networkAnime = state.source.getAnimeDetails(state.anime.toSAnime())
                updateAnime.awaitUpdateFromSource(state.anime, networkAnime, manualFetch)
            }
        } catch (e: Throwable) {
            // Ignore early hints "errors" that aren't handled by OkHttp
            if (e is HttpException && e.code == 103) return

            logcat(LogPriority.ERROR, e)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message = e.toString())
            }
        }
    }

    fun toggleFavorite() {
        toggleFavorite(
            onRemoved = {
                coroutineScope.launch {
                    if (!hasDownloads()) return@launch
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.delete_downloads_for_manga),
                        actionLabel = context.getString(R.string.action_delete),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        deleteDownloads()
                    }
                }
            },
        )
    }

    /**
     * Update favorite status of anime, (removes / adds) anime (to / from) library.
     */
    fun toggleFavorite(
        onRemoved: () -> Unit,
        checkDuplicate: Boolean = true,
    ) {
        val state = successState ?: return
        coroutineScope.launchIO {
            val anime = state.anime

            if (isFavoritedAnime) {
                // Remove from library
                if (updateAnime.awaitUpdateFavorite(anime.id, false)) {
                    // Remove covers and update last modified in db
                    if (anime.removeCovers() != anime) {
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
                        mutableState.update { state ->
                            when (state) {
                                AnimeScreenState.Loading -> state
                                is AnimeScreenState.Success -> state.copy(
                                    dialog = Dialog.DuplicateAnime(
                                        anime,
                                        duplicate,
                                    ),
                                )
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
                    }

                    // Automatic 'Default' or no categories
                    defaultCategoryId == 0L || categories.isEmpty() -> {
                        val result = updateAnime.awaitUpdateFavorite(anime.id, true)
                        if (!result) return@launchIO
                        moveAnimeToCategory(null)
                    }

                    // Choose a category
                    else -> {
                        isFromChangeCategory = true
                        promptChangeCategories()
                    }
                }

                // Finally match with enhanced tracking when available
                val source = state.source
                state.trackItems
                    .map { it.service }
                    .filterIsInstance<EnhancedAnimeTrackService>()
                    .filter { it.accept(source) }
                    .forEach { service ->
                        launchIO {
                            try {
                                service.match(anime)?.let { track ->
                                    (service as AnimeTrackService).registerTracking(track, animeId)
                                }
                            } catch (e: Exception) {
                                logcat(LogPriority.WARN, e) {
                                    "Could not match anime: ${anime.title} with service $service"
                                }
                            }
                        }
                    }
                if (autoOpenTrack) {
                    showTrackDialog()
                }
            }
        }
    }

    fun promptChangeCategories() {
        val state = successState ?: return
        val anime = state.anime
        coroutineScope.launch {
            val categories = getCategories()
            val selection = getAnimeCategoryIds(anime)
            mutableState.update { state ->
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
    private fun deleteDownloads() {
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
    private suspend fun getAnimeCategoryIds(anime: Anime): List<Long> {
        return getCategories.await(anime.id)
            .map { it.id }
    }

    fun moveAnimeToCategoriesAndAddToLibrary(anime: Anime, categories: List<Long>) {
        moveAnimeToCategory(categories)
        if (!anime.favorite) {
            coroutineScope.launchIO {
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
        coroutineScope.launchIO {
            setAnimeCategories.await(animeId, categoryIds)
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

    // Anime info - end

    // Episodes list - start

    private fun observeDownloads() {
        coroutineScope.launchIO {
            downloadManager.queue.statusFlow()
                .filter { it.anime.id == successState?.anime?.id }
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .collect {
                    withUIContext {
                        updateDownloadState(it)
                    }
                }
        }

        coroutineScope.launchIO {
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

    private fun List<Episode>.toEpisodeItems(
        context: Context,
        anime: Anime,
        dateRelativeTime: Int,
        dateFormat: DateFormat,
    ): List<EpisodeItem> {
        val isLocal = anime.isLocal()
        return map { episode ->
            val activeDownload = if (isLocal) {
                null
            } else {
                downloadManager.getQueuedDownloadOrNull(episode.id)
            }
            val downloaded = if (isLocal) {
                true
            } else {
                downloadManager.isEpisodeDownloaded(episode.name, episode.scanlator, anime.title, anime.source)
            }
            val downloadState = when {
                activeDownload != null -> activeDownload.status
                downloaded -> AnimeDownload.State.DOWNLOADED
                else -> AnimeDownload.State.NOT_DOWNLOADED
            }

            EpisodeItem(
                episode = episode,
                downloadState = downloadState,
                downloadProgress = activeDownload?.progress ?: 0,
                episodeTitleString = if (anime.displayMode == Anime.EPISODE_DISPLAY_NUMBER) {
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
                selected = episode.id in selectedEpisodeIds,
            )
        }
    }

    /**
     * Requests an updated list of episodes from the source.
     */
    private suspend fun fetchEpisodesFromSource(manualFetch: Boolean = false) {
        val state = successState ?: return
        try {
            withIOContext {
                val episodes = state.source.getEpisodeList(state.anime.toSAnime())

                val newEpisodes = syncEpisodesWithSource.await(
                    episodes,
                    state.anime,
                    state.source,
                )

                if (manualFetch) {
                    downloadNewEpisodes(newEpisodes)
                }
            }
        } catch (e: Throwable) {
            val message = if (e is NoEpisodesException) {
                context.getString(R.string.no_episodes_error)
            } else {
                logcat(LogPriority.ERROR, e)
                e.toString()
            }

            coroutineScope.launch {
                snackbarHostState.showSnackbar(message = message)
            }
        }
    }

    /**
     * Returns the next unseen episode or null if everything is seen.
     */
    fun getNextUnseenEpisode(): Episode? {
        val successState = successState ?: return null
        return successState.episodes.getNextUnseen(successState.anime)
    }

    fun getUnseenEpisodes(): List<Episode> {
        return successState?.processedEpisodes
            ?.filter { (episode, dlStatus) -> !episode.seen && dlStatus == AnimeDownload.State.NOT_DOWNLOADED }
            ?.map { it.episode }
            ?.toList()
            ?: emptyList()
    }

    fun getUnseenEpisodesSorted(): List<Episode> {
        val anime = successState?.anime ?: return emptyList()
        val episodes = getUnseenEpisodes().sortedWith(getEpisodeSort(anime))
        return if (anime.sortDescending()) episodes.reversed() else episodes
    }

    fun startDownload(
        episodes: List<Episode>,
        startNow: Boolean,
    ) {
        if (startNow) {
            val episodeId = episodes.singleOrNull()?.id ?: return
            downloadManager.startDownloadNow(episodeId)
        } else {
            downloadEpisodes(episodes)
        }
        if (!isFavoritedAnime) {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.snack_add_to_anime_library),
                    actionLabel = context.getString(R.string.action_add),
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed && !isFavoritedAnime) {
                    toggleFavorite()
                }
            }
        }
    }

    fun runEpisodeDownloadActions(
        items: List<EpisodeItem>,
        action: EpisodeDownloadAction,
    ) {
        when (action) {
            EpisodeDownloadAction.START -> {
                startDownload(items.map { it.episode }, false)
                if (items.any { it.downloadState == AnimeDownload.State.ERROR }) {
                    AnimeDownloadService.start(context)
                }
            }
            EpisodeDownloadAction.START_NOW -> {
                val episode = items.singleOrNull()?.episode ?: return
                startDownload(listOf(episode), true)
            }
            EpisodeDownloadAction.CANCEL -> {
                val episodeId = items.singleOrNull()?.episode?.id ?: return
                cancelDownload(episodeId)
            }
            EpisodeDownloadAction.DELETE -> {
                deleteEpisodes(items.map { it.episode })
            }
            EpisodeDownloadAction.START_ALT -> TODO()
        }
    }

    fun runDownloadAction(action: DownloadAction) {
        val episodesToDownload = when (action) {
            DownloadAction.NEXT_1_ITEM -> getUnseenEpisodesSorted().take(1)
            DownloadAction.NEXT_5_ITEMS -> getUnseenEpisodesSorted().take(5)
            DownloadAction.NEXT_10_ITEMS -> getUnseenEpisodesSorted().take(10)
            DownloadAction.CUSTOM -> {
                showDownloadCustomDialog()
                return
            }
            DownloadAction.UNVIEWED_ITEMS -> getUnseenEpisodes()
            DownloadAction.ALL_ITEMS -> successState?.episodes?.map { it.episode }
        }
        if (!episodesToDownload.isNullOrEmpty()) {
            startDownload(episodesToDownload, false)
        }
    }

    fun cancelDownload(episodeId: Long) {
        val activeDownload = downloadManager.getQueuedDownloadOrNull(episodeId) ?: return
        downloadManager.cancelQueuedDownloads(listOf(activeDownload))
        updateDownloadState(activeDownload.apply { status = AnimeDownload.State.NOT_DOWNLOADED })
    }

    fun markPreviousEpisodeSeen(pointer: Episode) {
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
    fun markEpisodesSeen(episodes: List<Episode>, seen: Boolean) {
        coroutineScope.launchIO {
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
    private fun downloadEpisodes(episodes: List<Episode>, alt: Boolean = false) {
        val anime = successState?.anime ?: return
        if (alt) {
            downloadManager.downloadEpisodesAlt(anime, episodes)
        } else {
            downloadManager.downloadEpisodes(anime, episodes)
        }
        toggleAllSelection(false)
    }

    /**
     * Bookmarks the given list of episodes.
     * @param episodes the list of episodes to bookmark.
     */
    fun bookmarkEpisodes(episodes: List<Episode>, bookmarked: Boolean) {
        coroutineScope.launchIO {
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
    fun deleteEpisodes(episodes: List<Episode>) {
        coroutineScope.launchNonCancellable {
            try {
                successState?.let { state ->
                    downloadManager.deleteEpisodes(
                        episodes,
                        state.anime,
                        state.source,
                    )
                }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
            }
        }
    }

    private fun downloadNewEpisodes(episodes: List<Episode>) {
        coroutineScope.launchNonCancellable {
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
    fun setUnseenFilter(state: TriStateFilter) {
        val anime = successState?.anime ?: return

        val flag = when (state) {
            TriStateFilter.DISABLED -> Anime.SHOW_ALL
            TriStateFilter.ENABLED_IS -> Anime.EPISODE_SHOW_UNSEEN
            TriStateFilter.ENABLED_NOT -> Anime.EPISODE_SHOW_SEEN
        }
        coroutineScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetUnseenFilter(anime, flag)
        }
    }

    /**
     * Sets the download filter and requests an UI update.
     * @param state whether to display only downloaded episodes or all episodes.
     */
    fun setDownloadedFilter(state: TriStateFilter) {
        val anime = successState?.anime ?: return

        val flag = when (state) {
            TriStateFilter.DISABLED -> Anime.SHOW_ALL
            TriStateFilter.ENABLED_IS -> Anime.EPISODE_SHOW_DOWNLOADED
            TriStateFilter.ENABLED_NOT -> Anime.EPISODE_SHOW_NOT_DOWNLOADED
        }

        coroutineScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetDownloadedFilter(anime, flag)
        }
    }

    /**
     * Sets the bookmark filter and requests an UI update.
     * @param state whether to display only bookmarked episodes or all episodes.
     */
    fun setBookmarkedFilter(state: TriStateFilter) {
        val anime = successState?.anime ?: return

        val flag = when (state) {
            TriStateFilter.DISABLED -> Anime.SHOW_ALL
            TriStateFilter.ENABLED_IS -> Anime.EPISODE_SHOW_BOOKMARKED
            TriStateFilter.ENABLED_NOT -> Anime.EPISODE_SHOW_NOT_BOOKMARKED
        }

        coroutineScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetBookmarkFilter(anime, flag)
        }
    }

    /**
     * Sets the active display mode.
     * @param mode the mode to set.
     */
    fun setDisplayMode(mode: Long) {
        val anime = successState?.anime ?: return

        coroutineScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetDisplayMode(anime, mode)
        }
    }

    /**
     * Sets the sorting method and requests an UI update.
     * @param sort the sorting mode.
     */
    fun setSorting(sort: Long) {
        val anime = successState?.anime ?: return

        coroutineScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetSortingModeOrFlipOrder(anime, sort)
        }
    }

    fun setCurrentSettingsAsDefault(applyToExisting: Boolean) {
        val anime = successState?.anime ?: return
        coroutineScope.launchNonCancellable {
            libraryPreferences.setEpisodeSettingsDefault(anime)
            if (applyToExisting) {
                setAnimeDefaultEpisodeFlags.awaitAll()
            }
            snackbarHostState.showSnackbar(message = context.getString(R.string.episode_settings_updated))
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
                val selectedIndex = successState.processedEpisodes.indexOfFirst { it.episode.id == item.episode.id }
                if (selectedIndex < 0) return@apply

                val selectedItem = get(selectedIndex)
                if ((selectedItem.selected && selected) || (!selectedItem.selected && !selected)) return@apply

                val firstSelection = none { it.selected }
                set(selectedIndex, selectedItem.copy(selected = selected))
                selectedEpisodeIds.addOrRemove(item.episode.id, selected)

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
                                selectedEpisodeIds.add(inbetweenItem.episode.id)
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
            successState.copy(episodes = newEpisodes)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        updateSuccessState { successState ->
            val newEpisodes = successState.episodes.map {
                selectedEpisodeIds.addOrRemove(it.episode.id, selected)
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
                selectedEpisodeIds.addOrRemove(it.episode.id, !it.selected)
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

        coroutineScope.launchIO {
            getTracks.subscribe(anime.id)
                .catch { logcat(LogPriority.ERROR, it) }
                .map { tracks ->
                    val dbTracks = tracks.map { it.toDbTrack() }
                    loggedServices
                        // Map to TrackItem
                        .map { service -> AnimeTrackItem(dbTracks.find { it.sync_id.toLong() == service.id }, service) }
                        // Show only if the service supports this manga's source
                        .filter { (it.service as? EnhancedAnimeTrackService)?.accept(source!!) ?: true }
                }
                .distinctUntilChanged()
                .collectLatest { trackItems ->
                    updateSuccessState { it.copy(trackItems = trackItems) }
                }
        }
    }

    // Track sheet - end

    fun getSourceOrStub(anime: Anime): AnimeSource {
        return sourceManager.getOrStub(anime.source)
    }

    sealed class Dialog {
        data class ChangeCategory(val anime: Anime, val initialSelection: List<CheckboxState<Category>>) : Dialog()
        data class DeleteEpisodes(val episodes: List<Episode>) : Dialog()
        data class DuplicateAnime(val anime: Anime, val duplicate: Anime) : Dialog()
        data class DownloadCustomAmount(val max: Int) : Dialog()
        object ChangeAnimeSkipIntro : Dialog()
        object SettingsSheet : Dialog()
        object TrackSheet : Dialog()
        object FullCover : Dialog()
    }

    fun dismissDialog() {
        mutableState.update { state ->
            when (state) {
                AnimeScreenState.Loading -> state
                is AnimeScreenState.Success -> state.copy(dialog = null)
            }
        }
    }
    private fun showDownloadCustomDialog() {
        val max = processedEpisodes?.count() ?: return
        mutableState.update { state ->
            when (state) {
                AnimeScreenState.Loading -> state
                is AnimeScreenState.Success -> state.copy(dialog = Dialog.DownloadCustomAmount(max))
            }
        }
    }

    fun showDeleteEpisodeDialog(chapters: List<Episode>) {
        mutableState.update { state ->
            when (state) {
                AnimeScreenState.Loading -> state
                is AnimeScreenState.Success -> state.copy(dialog = Dialog.DeleteEpisodes(chapters))
            }
        }
    }

    fun showSettingsDialog() {
        mutableState.update { state ->
            when (state) {
                AnimeScreenState.Loading -> state
                is AnimeScreenState.Success -> state.copy(dialog = Dialog.SettingsSheet)
            }
        }
    }

    fun showTrackDialog() {
        mutableState.update { state ->
            when (state) {
                AnimeScreenState.Loading -> state
                is AnimeScreenState.Success -> state.copy(dialog = Dialog.TrackSheet)
            }
        }
    }

    fun showCoverDialog() {
        mutableState.update { state ->
            when (state) {
                AnimeScreenState.Loading -> state
                is AnimeScreenState.Success -> state.copy(dialog = Dialog.FullCover)
            }
        }
    }

    fun showAnimeSkipIntroDialog() {
        mutableState.update { state ->
            when (state) {
                AnimeScreenState.Loading -> state
                is AnimeScreenState.Success -> state.copy(dialog = Dialog.ChangeAnimeSkipIntro)
            }
        }
    }
}

sealed class AnimeScreenState {
    @Immutable
    object Loading : AnimeScreenState()

    @Immutable
    data class Success(
        val anime: Anime,
        val source: AnimeSource,
        val isFromSource: Boolean,
        val episodes: List<EpisodeItem>,
        val trackItems: List<AnimeTrackItem> = emptyList(),
        val isRefreshingData: Boolean = false,
        val dialog: AnimeInfoScreenModel.Dialog? = null,
    ) : AnimeScreenState() {

        val processedEpisodes: Sequence<EpisodeItem>
            get() = episodes.applyFilters(anime)

        val trackingAvailable: Boolean
            get() = trackItems.isNotEmpty()

        val trackingCount: Int
            get() = trackItems.count { it.track != null }

        /**
         * Applies the view filters to the list of chapters obtained from the database.
         * @return an observable of the list of chapters filtered and sorted.
         */
        private fun List<EpisodeItem>.applyFilters(anime: Anime): Sequence<EpisodeItem> {
            val isLocalAnime = anime.isLocal()
            val unseenFilter = anime.unseenFilter
            val downloadedFilter = anime.downloadedFilter
            val bookmarkedFilter = anime.bookmarkedFilter
            return asSequence()
                .filter { (episode) ->
                    when (unseenFilter) {
                        TriStateFilter.DISABLED -> true
                        TriStateFilter.ENABLED_IS -> !episode.seen
                        TriStateFilter.ENABLED_NOT -> episode.seen
                    }
                }
                .filter { (episode) ->
                    when (bookmarkedFilter) {
                        TriStateFilter.DISABLED -> true
                        TriStateFilter.ENABLED_IS -> episode.bookmark
                        TriStateFilter.ENABLED_NOT -> !episode.bookmark
                    }
                }
                .filter {
                    when (downloadedFilter) {
                        TriStateFilter.DISABLED -> true
                        TriStateFilter.ENABLED_IS -> it.isDownloaded || isLocalAnime
                        TriStateFilter.ENABLED_NOT -> !it.isDownloaded && !isLocalAnime
                    }
                }
                .sortedWith { (episode1), (episode2) -> getEpisodeSort(anime).invoke(episode1, episode2) }
        }
    }
}

@Immutable
data class EpisodeItem(
    val episode: Episode,
    val downloadState: AnimeDownload.State,
    val downloadProgress: Int,

    val episodeTitleString: String,
    val dateUploadString: String?,
    val seenProgressString: String?,

    val selected: Boolean = false,
) {
    val isDownloaded = downloadState == AnimeDownload.State.DOWNLOADED
}

val episodeDecimalFormat = DecimalFormat(
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
