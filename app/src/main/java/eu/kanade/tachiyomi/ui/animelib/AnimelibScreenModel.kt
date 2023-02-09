package eu.kanade.tachiyomi.ui.animelib

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.prefs.CheckboxState
import eu.kanade.core.prefs.PreferenceMutableState
import eu.kanade.core.prefs.asState
import eu.kanade.core.util.fastDistinctBy
import eu.kanade.core.util.fastFilter
import eu.kanade.core.util.fastFilterNot
import eu.kanade.core.util.fastMapNotNull
import eu.kanade.core.util.fastPartition
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.category.interactor.GetAnimeCategories
import eu.kanade.domain.category.interactor.SetAnimeCategories
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.episode.interactor.SetSeenStatus
import eu.kanade.domain.animehistory.interactor.GetNextEpisodes
import eu.kanade.domain.library.model.LibrarySort
import eu.kanade.domain.library.model.sort
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.domain.anime.interactor.GetAnimelibAnime
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.AnimeUpdate
import eu.kanade.domain.anime.model.isLocal
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.domain.animetrack.interactor.GetTracksPerAnime
import eu.kanade.domain.episode.model.Episode
import eu.kanade.presentation.library.components.LibraryToolbarTitle
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadCache
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadManager
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.util.episode.getNextUnseen
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.Collator
import java.util.Collections
import java.util.Locale

/**
 * Typealias for the library anime, using the category as keys, and list of anime as values.
 */
typealias AnimelibMap = Map<Category, List<AnimelibItem>>

class AnimelibScreenModel(
    private val getAnimelibAnime: GetAnimelibAnime = Injekt.get(),
    private val getCategories: GetAnimeCategories = Injekt.get(),
    private val getTracksPerAnime: GetTracksPerAnime = Injekt.get(),
    private val getNextEpisodes: GetNextEpisodes = Injekt.get(),
    private val getEpisodesByAnimeId: GetEpisodeByAnimeId = Injekt.get(),
    private val setSeenStatus: SetSeenStatus = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    private val preferences: BasePreferences = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val coverCache: AnimeCoverCache = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val downloadManager: AnimeDownloadManager = Injekt.get(),
    private val downloadCache: AnimeDownloadCache = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
) : StateScreenModel<AnimelibScreenModel.State>(State()) {

    var activeCategoryIndex: Int by libraryPreferences.lastUsedAnimeCategory().asState(coroutineScope)

    init {
        coroutineScope.launchIO {
            combine(
                state.map { it.searchQuery }.distinctUntilChanged(),
                getLibraryFlow(),
                getTracksPerAnime.subscribe(),
                getTrackingFilterFlow(),
                downloadCache.changes,
            ) { searchQuery, library, tracks, loggedInTrackServices, _ ->
                library
                    .applyFilters(tracks, loggedInTrackServices)
                    .applySort()
                    .mapValues { (_, value) ->
                        if (searchQuery != null) {
                            // Filter query
                            value.filter { it.matches(searchQuery) }
                        } else {
                            // Don't do anything
                            value
                        }
                    }
            }
                .collectLatest {
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            library = it,
                        )
                    }
                }
        }

        combine(
            libraryPreferences.categoryTabs().changes(),
            libraryPreferences.categoryNumberOfItems().changes(),
            libraryPreferences.showContinueReadingButton().changes(),
        ) { a, b, c -> arrayOf(a, b, c) }
            .onEach { (showCategoryTabs, showAnimeCount, showAnimeContinueButton) ->
                mutableState.update { state ->
                    state.copy(
                        showCategoryTabs = showCategoryTabs,
                        showAnimeCount = showAnimeCount,
                        showAnimeContinueButton = showAnimeContinueButton,
                    )
                }
            }
            .launchIn(coroutineScope)

        combine(
            getAnimelibItemPreferencesFlow(),
            getTrackingFilterFlow(),
        ) { prefs, trackFilter ->
            val a = (
                prefs.filterDownloaded or
                    prefs.filterUnread or
                    prefs.filterStarted or
                    prefs.filterBookmarked or
                    prefs.filterCompleted
                ) != TriStateGroup.State.IGNORE.value
            val b = trackFilter.values.any { it != TriStateGroup.State.IGNORE.value }
            a || b
        }
            .distinctUntilChanged()
            .onEach {
                mutableState.update { state ->
                    state.copy(hasActiveFilters = it)
                }
            }
            .launchIn(coroutineScope)
    }

    /**
     * Applies library filters to the given map of anime.
     */
    private suspend fun AnimelibMap.applyFilters(
        trackMap: Map<Long, List<Long>>,
        loggedInTrackServices: Map<Long, Int>,
    ): AnimelibMap {
        val prefs = getAnimelibItemPreferencesFlow().first()
        val downloadedOnly = prefs.globalFilterDownloaded
        val filterDownloaded = prefs.filterDownloaded
        val filterUnread = prefs.filterUnread
        val filterStarted = prefs.filterStarted
        val filterBookmarked = prefs.filterBookmarked
        val filterCompleted = prefs.filterCompleted

        val isNotLoggedInAnyTrack = loggedInTrackServices.isEmpty()

        val excludedTracks = loggedInTrackServices.mapNotNull { if (it.value == TriStateGroup.State.EXCLUDE.value) it.key else null }
        val includedTracks = loggedInTrackServices.mapNotNull { if (it.value == TriStateGroup.State.INCLUDE.value) it.key else null }
        val trackFiltersIsIgnored = includedTracks.isEmpty() && excludedTracks.isEmpty()

        val filterFnDownloaded: (AnimelibItem) -> Boolean = downloaded@{
            if (!downloadedOnly && filterDownloaded == TriStateGroup.State.IGNORE.value) return@downloaded true

            val isDownloaded = it.animelibAnime.anime.isLocal() ||
                it.downloadCount > 0 ||
                downloadManager.getDownloadCount(it.animelibAnime.anime) > 0
            return@downloaded if (downloadedOnly || filterDownloaded == TriStateGroup.State.INCLUDE.value) {
                isDownloaded
            } else {
                !isDownloaded
            }
        }

        val filterFnUnread: (AnimelibItem) -> Boolean = unread@{
            if (filterUnread == TriStateGroup.State.IGNORE.value) return@unread true

            val isUnread = it.animelibAnime.unseenCount > 0
            return@unread if (filterUnread == TriStateGroup.State.INCLUDE.value) {
                isUnread
            } else {
                !isUnread
            }
        }

        val filterFnStarted: (AnimelibItem) -> Boolean = started@{
            if (filterStarted == TriStateGroup.State.IGNORE.value) return@started true

            val hasStarted = it.animelibAnime.hasStarted
            return@started if (filterStarted == TriStateGroup.State.INCLUDE.value) {
                hasStarted
            } else {
                !hasStarted
            }
        }

        val filterFnBookmarked: (AnimelibItem) -> Boolean = bookmarked@{
            if (filterBookmarked == TriStateGroup.State.IGNORE.value) return@bookmarked true

            val hasBookmarks = it.animelibAnime.hasBookmarks
            return@bookmarked if (filterBookmarked == TriStateGroup.State.INCLUDE.value) {
                hasBookmarks
            } else {
                !hasBookmarks
            }
        }

        val filterFnCompleted: (AnimelibItem) -> Boolean = completed@{
            if (filterCompleted == TriStateGroup.State.IGNORE.value) return@completed true

            val isCompleted = it.animelibAnime.anime.status.toInt() == SAnime.COMPLETED
            return@completed if (filterCompleted == TriStateGroup.State.INCLUDE.value) {
                isCompleted
            } else {
                !isCompleted
            }
        }

        val filterFnTracking: (AnimelibItem) -> Boolean = tracking@{ item ->
            if (isNotLoggedInAnyTrack || trackFiltersIsIgnored) return@tracking true

            val nimeTracks = trackMap[item.animelibAnime.id].orEmpty()

            val exclude = nimeTracks.fastFilter { it in excludedTracks }
            val include = nimeTracks.fastFilter { it in includedTracks }

            // TODO: Simplify the filter logic
            if (includedTracks.isNotEmpty() && excludedTracks.isNotEmpty()) {
                return@tracking if (exclude.isNotEmpty()) false else include.isNotEmpty()
            }

            if (excludedTracks.isNotEmpty()) return@tracking exclude.isEmpty()

            if (includedTracks.isNotEmpty()) return@tracking include.isNotEmpty()

            return@tracking false
        }

        val filterFn: (AnimelibItem) -> Boolean = filter@{
            return@filter !(
                !filterFnDownloaded(it) ||
                    !filterFnUnread(it) ||
                    !filterFnStarted(it) ||
                    !filterFnBookmarked(it) ||
                    !filterFnCompleted(it) ||
                    !filterFnTracking(it)
                )
        }

        return this.mapValues { entry -> entry.value.fastFilter(filterFn) }
    }

    /**
     * Applies library sorting to the given map of anime.
     */
    private fun AnimelibMap.applySort(): AnimelibMap {
        val locale = Locale.getDefault()
        val collator = Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
        val sortAlphabetically: (AnimelibItem, AnimelibItem) -> Int = { i1, i2 ->
            collator.compare(i1.animelibAnime.anime.title.lowercase(locale), i2.animelibAnime.anime.title.lowercase(locale))
        }

        val sortFn: (AnimelibItem, AnimelibItem) -> Int = { i1, i2 ->
            val sort = keys.find { it.id == i1.animelibAnime.category }!!.sort
            when (sort.type) {
                LibrarySort.Type.Alphabetical -> {
                    sortAlphabetically(i1, i2)
                }
                LibrarySort.Type.LastRead -> {
                    i1.animelibAnime.lastSeen.compareTo(i2.animelibAnime.lastSeen)
                }
                LibrarySort.Type.LastUpdate -> {
                    i1.animelibAnime.anime.lastUpdate.compareTo(i2.animelibAnime.anime.lastUpdate)
                }
                LibrarySort.Type.UnreadCount -> when {
                    // Ensure unread content comes first
                    i1.animelibAnime.unseenCount == i2.animelibAnime.unseenCount -> 0
                    i1.animelibAnime.unseenCount == 0L -> if (sort.isAscending) 1 else -1
                    i2.animelibAnime.unseenCount == 0L -> if (sort.isAscending) -1 else 1
                    else -> i1.animelibAnime.unseenCount.compareTo(i2.animelibAnime.unseenCount)
                }
                LibrarySort.Type.TotalChapters -> {
                    i1.animelibAnime.totalEpisodes.compareTo(i2.animelibAnime.totalEpisodes)
                }
                LibrarySort.Type.LatestChapter -> {
                    i1.animelibAnime.latestUpload.compareTo(i2.animelibAnime.latestUpload)
                }
                LibrarySort.Type.ChapterFetchDate -> {
                    i1.animelibAnime.episodeFetchedAt.compareTo(i2.animelibAnime.episodeFetchedAt)
                }
                LibrarySort.Type.DateAdded -> {
                    i1.animelibAnime.anime.dateAdded.compareTo(i2.animelibAnime.anime.dateAdded)
                }
            }
        }

        return this.mapValues { entry ->
            val comparator = if (keys.find { it.id == entry.key.id }!!.sort.isAscending) {
                Comparator(sortFn)
            } else {
                Collections.reverseOrder(sortFn)
            }

            entry.value.sortedWith(comparator.thenComparator(sortAlphabetically))
        }
    }

    private fun getAnimelibItemPreferencesFlow(): Flow<ItemPreferences> {
        return combine(
            libraryPreferences.downloadBadge().changes(),
            libraryPreferences.unreadBadge().changes(),
            libraryPreferences.localBadge().changes(),
            libraryPreferences.languageBadge().changes(),

            preferences.downloadedOnly().changes(),
            libraryPreferences.filterDownloaded().changes(),
            libraryPreferences.filterUnread().changes(),
            libraryPreferences.filterStarted().changes(),
            libraryPreferences.filterBookmarked().changes(),
            libraryPreferences.filterCompleted().changes(),
            transform = {
                ItemPreferences(
                    downloadBadge = it[0] as Boolean,
                    unseenBadge = it[1] as Boolean,
                    localBadge = it[2] as Boolean,
                    languageBadge = it[3] as Boolean,
                    globalFilterDownloaded = it[4] as Boolean,
                    filterDownloaded = it[5] as Int,
                    filterUnread = it[6] as Int,
                    filterStarted = it[7] as Int,
                    filterBookmarked = it[8] as Int,
                    filterCompleted = it[9] as Int,
                )
            },
        )
    }

    /**
     * Get the categories and all its anime from the database.
     */
    private fun getLibraryFlow(): Flow<AnimelibMap> {
        val animelibAnimesFlow = combine(
            getAnimelibAnime.subscribe(),
            getAnimelibItemPreferencesFlow(),
            downloadCache.changes,
        ) { animelibAnimeList, prefs, _ ->
            animelibAnimeList
                .map { animelibAnime ->
                    // Display mode based on user preference: take it from global library setting or category
                    AnimelibItem(animelibAnime).apply {
                        downloadCount = if (prefs.downloadBadge) {
                            downloadManager.getDownloadCount(animelibAnime.anime).toLong()
                        } else {
                            0
                        }
                        unseenCount = if (prefs.unseenBadge) animelibAnime.unseenCount else 0
                        isLocal = if (prefs.localBadge) animelibAnime.anime.isLocal() else false
                        sourceLanguage = if (prefs.languageBadge) {
                            sourceManager.getOrStub(animelibAnime.anime.source).lang
                        } else {
                            ""
                        }
                    }
                }
                .groupBy { it.animelibAnime.category }
        }

        return combine(getCategories.subscribe(), animelibAnimesFlow) { categories, animelibAnime ->
            val displayCategories = if (animelibAnime.isNotEmpty() && !animelibAnime.containsKey(0)) {
                categories.fastFilterNot { it.isSystemCategory }
            } else {
                categories
            }

            displayCategories.associateWith { animelibAnime[it.id] ?: emptyList() }
        }
    }

    /**
     * Flow of tracking filter preferences
     *
     * @return map of track id with the filter value
     */
    private fun getTrackingFilterFlow(): Flow<Map<Long, Int>> {
        val loggedServices = trackManager.services.filter { it.isLogged }
        return if (loggedServices.isNotEmpty()) {
            val prefFlows = loggedServices
                .map { libraryPreferences.filterTracking(it.id.toInt()).changes() }
                .toTypedArray()
            combine(*prefFlows) {
                loggedServices
                    .mapIndexed { index, trackService -> trackService.id to it[index] }
                    .toMap()
            }
        } else {
            flowOf(emptyMap())
        }
    }

    /**
     * Returns the common categories for the given list of anime.
     *
     * @param animes the list of anime.
     */
    private suspend fun getCommonCategories(animes: List<Anime>): Collection<Category> {
        if (animes.isEmpty()) return emptyList()
        return animes
            .map { getCategories.await(it.id).toSet() }
            .reduce { set1, set2 -> set1.intersect(set2) }
    }

    suspend fun getNextUnseenEpisode(anime: Anime): Episode? {
        return getEpisodesByAnimeId.await(anime.id).getNextUnseen(anime, downloadManager)
    }

    /**
     * Returns the mix (non-common) categories for the given list of anime.
     *
     * @param animes the list of anime.
     */
    private suspend fun getMixCategories(animes: List<Anime>): Collection<Category> {
        if (animes.isEmpty()) return emptyList()
        val nimeCategories = animes.map { getCategories.await(it.id).toSet() }
        val common = nimeCategories.reduce { set1, set2 -> set1.intersect(set2) }
        return nimeCategories.flatten().distinct().subtract(common)
    }

    fun runDownloadActionSelection(action: DownloadAction) {
        val selection = state.value.selection
        val animes = selection.map { it.anime }.toList()
        when (action) {
            DownloadAction.NEXT_1_CHAPTER -> downloadUnseenEpisodes(animes, 1)
            DownloadAction.NEXT_5_CHAPTERS -> downloadUnseenEpisodes(animes, 5)
            DownloadAction.NEXT_10_CHAPTERS -> downloadUnseenEpisodes(animes, 10)
            DownloadAction.UNREAD_CHAPTERS -> downloadUnseenEpisodes(animes, null)
            DownloadAction.CUSTOM -> {
                mutableState.update { state ->
                    state.copy(
                        dialog = Dialog.DownloadCustomAmount(
                            animes,
                            selection.maxOf { it.unseenCount }.toInt(),
                        ),
                    )
                }
                return
            }
            else -> {}
        }
        clearSelection()
    }

    /**
     * Queues the amount specified of unread episodes from the list of animes given.
     *
     * @param animes the list of anime.
     * @param amount the amount to queue or null to queue all
     */
    fun downloadUnseenEpisodes(animes: List<Anime>, amount: Int?) {
        coroutineScope.launchNonCancellable {
            animes.forEach { anime ->
                val episodes = getNextEpisodes.await(anime.id)
                    .fastFilterNot { episode ->
                        downloadManager.getQueuedDownloadOrNull(episode.id) != null ||
                            downloadManager.isEpisodeDownloaded(
                                episode.name,
                                episode.scanlator,
                                anime.title,
                                anime.source,
                            )
                    }
                    .let { if (amount != null) it.take(amount) else it }

                downloadManager.downloadEpisodes(anime, episodes)
            }
        }
    }

    /**
     * Marks animes' episodes seen status.
     */
    fun markSeenSelection(seen: Boolean) {
        val animes = state.value.selection.toList()
        coroutineScope.launchNonCancellable {
            animes.forEach { anime ->
                setSeenStatus.await(
                    anime = anime.anime,
                    seen = seen,
                )
            }
        }
        clearSelection()
    }

    /**
     * Remove the selected anime.
     *
     * @param animeList the list of anime to delete.
     * @param deleteFromLibrary whether to delete anime from library.
     * @param deleteEpisodes whether to delete downloaded episodes.
     */
    fun removeAnimes(animeList: List<Anime>, deleteFromLibrary: Boolean, deleteEpisodes: Boolean) {
        coroutineScope.launchNonCancellable {
            val animeToDelete = animeList.distinctBy { it.id }

            if (deleteFromLibrary) {
                val toDelete = animeToDelete.map {
                    it.removeCovers(coverCache)
                    AnimeUpdate(
                        favorite = false,
                        id = it.id,
                    )
                }
                updateAnime.awaitAll(toDelete)
            }

            if (deleteEpisodes) {
                animeToDelete.forEach { anime ->
                    val source = sourceManager.get(anime.source) as? AnimeHttpSource
                    if (source != null) {
                        downloadManager.deleteAnime(anime, source)
                    }
                }
            }
        }
    }

    /**
     * Bulk update categories of anime using old and new common categories.
     *
     * @param animeList the list of anime to move.
     * @param addCategories the categories to add for all animes.
     * @param removeCategories the categories to remove in all animes.
     */
    fun setAnimeCategories(animeList: List<Anime>, addCategories: List<Long>, removeCategories: List<Long>) {
        coroutineScope.launchNonCancellable {
            animeList.forEach { anime ->
                val categoryIds = getCategories.await(anime.id)
                    .map { it.id }
                    .subtract(removeCategories.toSet())
                    .plus(addCategories)
                    .toList()

                setAnimeCategories.await(anime.id, categoryIds)
            }
        }
    }

    fun getColumnsPreferenceForCurrentOrientation(isLandscape: Boolean): PreferenceMutableState<Int> {
        return (if (isLandscape) libraryPreferences.landscapeColumns() else libraryPreferences.portraitColumns()).asState(coroutineScope)
    }

    suspend fun getRandomAnimelibItemForCurrentCategory(): AnimelibItem? {
        return withIOContext {
            state.value
                .getAnimelibItemsByCategoryId(state.value.categories[activeCategoryIndex].id)
                ?.randomOrNull()
        }
    }

    fun clearSelection() {
        mutableState.update { it.copy(selection = emptyList()) }
    }

    fun toggleSelection(anime: AnimelibAnime) {
        mutableState.update { state ->
            val newSelection = state.selection.toMutableList().apply {
                if (fastAny { it.id == anime.id }) {
                    removeAll { it.id == anime.id }
                } else {
                    add(anime)
                }
            }
            state.copy(selection = newSelection)
        }
    }

    /**
     * Selects all nimes between and including the given anime and the last pressed anime from the
     * same category as the given anime
     */
    fun toggleRangeSelection(anime: AnimelibAnime) {
        mutableState.update { state ->
            val newSelection = state.selection.toMutableList().apply {
                val lastSelected = lastOrNull()
                if (lastSelected?.category != anime.category) {
                    add(anime)
                    return@apply
                }

                val items = state.getAnimelibItemsByCategoryId(anime.category)
                    ?.fastMap { it.animelibAnime }.orEmpty()
                val lastAnimeIndex = items.indexOf(lastSelected)
                val curAnimeIndex = items.indexOf(anime)

                val selectedIds = fastMap { it.id }
                val selectionRange = when {
                    lastAnimeIndex < curAnimeIndex -> IntRange(lastAnimeIndex, curAnimeIndex)
                    curAnimeIndex < lastAnimeIndex -> IntRange(curAnimeIndex, lastAnimeIndex)
                    // We shouldn't reach this point
                    else -> return@apply
                }
                val newSelections = selectionRange.mapNotNull { index ->
                    items[index].takeUnless { it.id in selectedIds }
                }
                addAll(newSelections)
            }
            state.copy(selection = newSelection)
        }
    }

    fun selectAll(index: Int) {
        mutableState.update { state ->
            val newSelection = state.selection.toMutableList().apply {
                val categoryId = state.categories.getOrNull(index)?.id ?: -1
                val selectedIds = fastMap { it.id }
                state.getAnimelibItemsByCategoryId(categoryId)
                    ?.fastMapNotNull { item ->
                        item.animelibAnime.takeUnless { it.id in selectedIds }
                    }
                    ?.let { addAll(it) }
            }
            state.copy(selection = newSelection)
        }
    }

    fun invertSelection(index: Int) {
        mutableState.update { state ->
            val newSelection = state.selection.toMutableList().apply {
                val categoryId = state.categories[index].id
                val items = state.getAnimelibItemsByCategoryId(categoryId)?.fastMap { it.animelibAnime }.orEmpty()
                val selectedIds = fastMap { it.id }
                val (toRemove, toAdd) = items.fastPartition { it.id in selectedIds }
                val toRemoveIds = toRemove.fastMap { it.id }
                removeAll { it.id in toRemoveIds }
                addAll(toAdd)
            }
            state.copy(selection = newSelection)
        }
    }

    fun search(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun openChangeCategoryDialog() {
        coroutineScope.launchIO {
            // Create a copy of selected anime
            val animeList = state.value.selection.map { it.anime }

            // Hide the default category because it has a different behavior than the ones from db.
            val categories = state.value.categories.filter { it.id != 0L }

            // Get indexes of the common categories to preselect.
            val common = getCommonCategories(animeList)
            // Get indexes of the mix categories to preselect.
            val mix = getMixCategories(animeList)
            val preselected = categories.map {
                when (it) {
                    in common -> CheckboxState.State.Checked(it)
                    in mix -> CheckboxState.TriState.Exclude(it)
                    else -> CheckboxState.State.None(it)
                }
            }
            mutableState.update { it.copy(dialog = Dialog.ChangeCategory(animeList, preselected)) }
        }
    }

    fun openDeleteAnimeDialog() {
        val nimeList = state.value.selection.map { it.anime }
        mutableState.update { it.copy(dialog = Dialog.DeleteAnime(nimeList)) }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    sealed class Dialog {
        data class ChangeCategory(val anime: List<Anime>, val initialSelection: List<CheckboxState<Category>>) : Dialog()
        data class DeleteAnime(val anime: List<Anime>) : Dialog()
        data class DownloadCustomAmount(val anime: List<Anime>, val max: Int) : Dialog()
    }

    @Immutable
    private data class ItemPreferences(
        val downloadBadge: Boolean,
        val unseenBadge: Boolean,
        val localBadge: Boolean,
        val languageBadge: Boolean,

        val globalFilterDownloaded: Boolean,
        val filterDownloaded: Int,
        val filterUnread: Int,
        val filterStarted: Int,
        val filterBookmarked: Int,
        val filterCompleted: Int,
    )

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val library: AnimelibMap = emptyMap(),
        val searchQuery: String? = null,
        val selection: List<AnimelibAnime> = emptyList(),
        val hasActiveFilters: Boolean = false,
        val showCategoryTabs: Boolean = false,
        val showAnimeCount: Boolean = false,
        val showAnimeContinueButton: Boolean = false,
        val dialog: Dialog? = null,
    ) {
        private val libraryCount by lazy {
            library.values
                .flatten()
                .fastDistinctBy { it.animelibAnime.anime.id }
                .size
        }

        val isLibraryEmpty by lazy { libraryCount == 0 }

        val selectionMode = selection.isNotEmpty()

        val categories = library.keys.toList()

        fun getAnimelibItemsByCategoryId(categoryId: Long): List<AnimelibItem>? {
            return library.firstNotNullOfOrNull { (k, v) -> v.takeIf { k.id == categoryId } }
        }

        fun getAnimelibItemsByPage(page: Int): List<AnimelibItem> {
            return library.values.toTypedArray().getOrNull(page) ?: emptyList()
        }

        fun getAnimeCountForCategory(category: Category): Int? {
            return if (showAnimeCount || !searchQuery.isNullOrEmpty()) library[category]?.size else null
        }

        fun getToolbarTitle(
            defaultTitle: String,
            defaultCategoryTitle: String,
            page: Int,
        ): LibraryToolbarTitle {
            val category = categories.getOrNull(page) ?: return LibraryToolbarTitle(defaultTitle)
            val categoryName = category.let {
                if (it.isSystemCategory) defaultCategoryTitle else it.name
            }
            val title = if (showCategoryTabs) defaultTitle else categoryName
            val count = when {
                !showAnimeCount -> null
                !showCategoryTabs -> getAnimeCountForCategory(category)
                // Whole library count
                else -> libraryCount
            }

            return LibraryToolbarTitle(title, count)
        }
    }
}
