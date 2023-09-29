package eu.kanade.tachiyomi.ui.library.manga

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.core.preference.asState
import eu.kanade.core.util.fastDistinctBy
import eu.kanade.core.util.fastFilter
import eu.kanade.core.util.fastFilterNot
import eu.kanade.core.util.fastMapNotNull
import eu.kanade.core.util.fastPartition
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.entries.manga.interactor.UpdateManga
import eu.kanade.domain.items.chapter.interactor.SetReadStatus
import eu.kanade.presentation.components.SEARCH_DEBOUNCE_MILLIS
import eu.kanade.presentation.entries.DownloadAction
import eu.kanade.presentation.library.LibraryToolbarTitle
import eu.kanade.tachiyomi.data.cache.MangaCoverCache
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadCache
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import eu.kanade.tachiyomi.data.track.MangaTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.chapter.getNextUnread
import eu.kanade.tachiyomi.util.removeCovers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import tachiyomi.core.preference.CheckboxState
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.domain.category.manga.interactor.GetVisibleMangaCategories
import tachiyomi.domain.category.manga.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.TriStateFilter
import tachiyomi.domain.entries.applyFilter
import tachiyomi.domain.entries.manga.interactor.GetLibraryManga
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.entries.manga.model.MangaUpdate
import tachiyomi.domain.history.manga.interactor.GetNextChapters
import tachiyomi.domain.items.chapter.interactor.GetChapterByMangaId
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.library.manga.LibraryManga
import tachiyomi.domain.library.manga.model.MangaLibrarySort
import tachiyomi.domain.library.manga.model.sort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.manga.service.MangaSourceManager
import tachiyomi.domain.track.manga.interactor.GetTracksPerManga
import tachiyomi.source.local.entries.manga.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.Collator
import java.util.Collections
import java.util.Locale

/**
 * Typealias for the library manga, using the category as keys, and list of manga as values.
 */
typealias MangaLibraryMap = Map<Category, List<MangaLibraryItem>>

class MangaLibraryScreenModel(
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val getCategories: GetVisibleMangaCategories = Injekt.get(),
    private val getTracksPerManga: GetTracksPerManga = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    private val getChaptersByMangaId: GetChapterByMangaId = Injekt.get(),
    private val setReadStatus: SetReadStatus = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
    private val preferences: BasePreferences = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val coverCache: MangaCoverCache = Injekt.get(),
    private val sourceManager: MangaSourceManager = Injekt.get(),
    private val downloadManager: MangaDownloadManager = Injekt.get(),
    private val downloadCache: MangaDownloadCache = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
) : StateScreenModel<MangaLibraryScreenModel.State>(State()) {

    var activeCategoryIndex: Int by libraryPreferences.lastUsedMangaCategory().asState(coroutineScope)

    init {
        coroutineScope.launchIO {
            combine(
                state.map { it.searchQuery }.debounce(SEARCH_DEBOUNCE_MILLIS),
                getLibraryFlow(),
                getTracksPerManga.subscribe(),
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
            libraryPreferences.showContinueViewingButton().changes(),
        ) { a, b, c -> arrayOf(a, b, c) }
            .onEach { (showCategoryTabs, showMangaCount, showMangaContinueButton) ->
                mutableState.update { state ->
                    state.copy(
                        showCategoryTabs = showCategoryTabs,
                        showMangaCount = showMangaCount,
                        showMangaContinueButton = showMangaContinueButton,
                    )
                }
            }
            .launchIn(coroutineScope)

        combine(
            getLibraryItemPreferencesFlow(),
            getTrackingFilterFlow(),
        ) { prefs, trackFilter ->
            (
                listOf(
                    prefs.filterDownloaded,
                    prefs.filterUnread,
                    prefs.filterStarted,
                    prefs.filterBookmarked,
                    prefs.filterCompleted,
                ) + trackFilter.values
                ).any { it != TriStateFilter.DISABLED }
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
     * Applies library filters to the given map of manga.
     */
    private suspend fun MangaLibraryMap.applyFilters(
        trackMap: Map<Long, List<Long>>,
        loggedInTrackServices: Map<Long, TriStateFilter>,
    ): MangaLibraryMap {
        val prefs = getLibraryItemPreferencesFlow().first()
        val downloadedOnly = prefs.globalFilterDownloaded
        val filterDownloaded =
            if (downloadedOnly) TriStateFilter.ENABLED_IS else prefs.filterDownloaded
        val filterUnread = prefs.filterUnread
        val filterStarted = prefs.filterStarted
        val filterBookmarked = prefs.filterBookmarked
        val filterCompleted = prefs.filterCompleted

        val isNotLoggedInAnyTrack = loggedInTrackServices.isEmpty()

        val excludedTracks = loggedInTrackServices.mapNotNull { if (it.value == TriStateFilter.ENABLED_NOT) it.key else null }
        val includedTracks = loggedInTrackServices.mapNotNull { if (it.value == TriStateFilter.ENABLED_IS) it.key else null }
        val trackFiltersIsIgnored = includedTracks.isEmpty() && excludedTracks.isEmpty()

        val filterFnDownloaded: (MangaLibraryItem) -> Boolean = {
            applyFilter(filterDownloaded) {
                it.libraryManga.manga.isLocal() ||
                    it.downloadCount > 0 ||
                    downloadManager.getDownloadCount(it.libraryManga.manga) > 0
            }
        }

        val filterFnUnread: (MangaLibraryItem) -> Boolean = {
            applyFilter(filterUnread) { it.libraryManga.unreadCount > 0 }
        }

        val filterFnStarted: (MangaLibraryItem) -> Boolean = {
            applyFilter(filterStarted) { it.libraryManga.hasStarted }
        }

        val filterFnBookmarked: (MangaLibraryItem) -> Boolean = {
            applyFilter(filterBookmarked) { it.libraryManga.hasBookmarks }
        }

        val filterFnCompleted: (MangaLibraryItem) -> Boolean = {
            applyFilter(filterCompleted) { it.libraryManga.manga.status.toInt() == SManga.COMPLETED }
        }

        val filterFnTracking: (MangaLibraryItem) -> Boolean = tracking@{ item ->
            if (isNotLoggedInAnyTrack || trackFiltersIsIgnored) return@tracking true

            val mangaTracks = trackMap[item.libraryManga.id].orEmpty()

            val isExcluded = excludedTracks.isNotEmpty() && mangaTracks.fastAny { it in excludedTracks }
            val isIncluded = includedTracks.isEmpty() || mangaTracks.fastAny { it in includedTracks }

            return@tracking !isExcluded && isIncluded
        }

        val filterFn: (MangaLibraryItem) -> Boolean = {
            filterFnDownloaded(it) &&
                filterFnUnread(it) &&
                filterFnStarted(it) &&
                filterFnBookmarked(it) &&
                filterFnCompleted(it) &&
                filterFnTracking(it)
        }

        return this.mapValues { entry -> entry.value.fastFilter(filterFn) }
    }

    /**
     * Applies library sorting to the given map of manga.
     */
    private fun MangaLibraryMap.applySort(): MangaLibraryMap {
        val locale = Locale.getDefault()
        val collator = Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
        val sortAlphabetically: (MangaLibraryItem, MangaLibraryItem) -> Int = { i1, i2 ->
            collator.compare(i1.libraryManga.manga.title.lowercase(locale), i2.libraryManga.manga.title.lowercase(locale))
        }

        val sortFn: (MangaLibraryItem, MangaLibraryItem) -> Int = { i1, i2 ->
            val sort = keys.find { it.id == i1.libraryManga.category }!!.sort
            when (sort.type) {
                MangaLibrarySort.Type.Alphabetical -> {
                    sortAlphabetically(i1, i2)
                }
                MangaLibrarySort.Type.LastRead -> {
                    i1.libraryManga.lastRead.compareTo(i2.libraryManga.lastRead)
                }
                MangaLibrarySort.Type.LastUpdate -> {
                    i1.libraryManga.manga.lastUpdate.compareTo(i2.libraryManga.manga.lastUpdate)
                }
                MangaLibrarySort.Type.UnreadCount -> when {
                    // Ensure unread content comes first
                    i1.libraryManga.unreadCount == i2.libraryManga.unreadCount -> 0
                    i1.libraryManga.unreadCount == 0L -> if (sort.isAscending) 1 else -1
                    i2.libraryManga.unreadCount == 0L -> if (sort.isAscending) -1 else 1
                    else -> i1.libraryManga.unreadCount.compareTo(i2.libraryManga.unreadCount)
                }
                MangaLibrarySort.Type.TotalChapters -> {
                    i1.libraryManga.totalChapters.compareTo(i2.libraryManga.totalChapters)
                }
                MangaLibrarySort.Type.LatestChapter -> {
                    i1.libraryManga.latestUpload.compareTo(i2.libraryManga.latestUpload)
                }
                MangaLibrarySort.Type.ChapterFetchDate -> {
                    i1.libraryManga.chapterFetchedAt.compareTo(i2.libraryManga.chapterFetchedAt)
                }
                MangaLibrarySort.Type.DateAdded -> {
                    i1.libraryManga.manga.dateAdded.compareTo(i2.libraryManga.manga.dateAdded)
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

    private fun getLibraryItemPreferencesFlow(): Flow<ItemPreferences> {
        return combine(
            libraryPreferences.downloadBadge().changes(),
            libraryPreferences.localBadge().changes(),
            libraryPreferences.languageBadge().changes(),

            preferences.downloadedOnly().changes(),
            libraryPreferences.filterDownloadedManga().changes(),
            libraryPreferences.filterUnread().changes(),
            libraryPreferences.filterStartedManga().changes(),
            libraryPreferences.filterBookmarkedManga().changes(),
            libraryPreferences.filterCompletedManga().changes(),
            transform = {
                ItemPreferences(
                    downloadBadge = it[0] as Boolean,
                    localBadge = it[1] as Boolean,
                    languageBadge = it[2] as Boolean,
                    globalFilterDownloaded = it[3] as Boolean,
                    filterDownloaded = it[4] as TriStateFilter,
                    filterUnread = it[5] as TriStateFilter,
                    filterStarted = it[6] as TriStateFilter,
                    filterBookmarked = it[7] as TriStateFilter,
                    filterCompleted = it[8] as TriStateFilter,
                )
            },
        )
    }

    /**
     * Get the categories and all its manga from the database.
     */
    private fun getLibraryFlow(): Flow<MangaLibraryMap> {
        val libraryMangasFlow = combine(
            getLibraryManga.subscribe(),
            getLibraryItemPreferencesFlow(),
            downloadCache.changes,
        ) { libraryMangaList, prefs, _ ->
            libraryMangaList
                .map { libraryManga ->
                    // Display mode based on user preference: take it from global library setting or category
                    MangaLibraryItem(
                        libraryManga,
                        downloadCount = if (prefs.downloadBadge) {
                            downloadManager.getDownloadCount(libraryManga.manga).toLong()
                        } else {
                            0
                        },
                        unreadCount = libraryManga.unreadCount,
                        isLocal = if (prefs.localBadge) libraryManga.manga.isLocal() else false,
                        sourceLanguage = if (prefs.languageBadge) {
                            sourceManager.getOrStub(libraryManga.manga.source).lang
                        } else {
                            ""
                        },
                    )
                }
                .groupBy { it.libraryManga.category }
        }

        return combine(getCategories.subscribe(), libraryMangasFlow) { categories, libraryManga ->
            val displayCategories = if (libraryManga.isNotEmpty() && !libraryManga.containsKey(0)) {
                categories.fastFilterNot { it.isSystemCategory }
            } else {
                categories
            }

            displayCategories.associateWith { libraryManga[it.id].orEmpty() }
        }
    }

    /**
     * Flow of tracking filter preferences
     *
     * @return map of track id with the filter value
     */
    private fun getTrackingFilterFlow(): Flow<Map<Long, TriStateFilter>> {
        val loggedServices = trackManager.services.filter { it.isLogged && it is MangaTrackService }
        return if (loggedServices.isNotEmpty()) {
            val prefFlows = loggedServices
                .map { libraryPreferences.filterTrackedManga(it.id.toInt()).changes() }
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
     * Returns the common categories for the given list of manga.
     *
     * @param mangas the list of manga.
     */
    private suspend fun getCommonCategories(mangas: List<Manga>): Collection<Category> {
        if (mangas.isEmpty()) return emptyList()
        return mangas
            .map { getCategories.await(it.id).toSet() }
            .reduce { set1, set2 -> set1.intersect(set2) }
    }

    suspend fun getNextUnreadChapter(manga: Manga): Chapter? {
        return getChaptersByMangaId.await(manga.id).getNextUnread(manga, downloadManager)
    }

    /**
     * Returns the mix (non-common) categories for the given list of manga.
     *
     * @param mangas the list of manga.
     */
    private suspend fun getMixCategories(mangas: List<Manga>): Collection<Category> {
        if (mangas.isEmpty()) return emptyList()
        val mangaCategories = mangas.map { getCategories.await(it.id).toSet() }
        val common = mangaCategories.reduce { set1, set2 -> set1.intersect(set2) }
        return mangaCategories.flatten().distinct().subtract(common)
    }

    fun runDownloadActionSelection(action: DownloadAction) {
        val selection = state.value.selection
        val mangas = selection.map { it.manga }.toList()
        when (action) {
            DownloadAction.NEXT_1_ITEM -> downloadUnreadChapters(mangas, 1)
            DownloadAction.NEXT_5_ITEMS -> downloadUnreadChapters(mangas, 5)
            DownloadAction.NEXT_10_ITEMS -> downloadUnreadChapters(mangas, 10)
            DownloadAction.NEXT_25_ITEMS -> downloadUnreadChapters(mangas, 25)
            DownloadAction.UNVIEWED_ITEMS -> downloadUnreadChapters(mangas, null)
        }
        clearSelection()
    }

    /**
     * Queues the amount specified of unread chapters from the list of mangas given.
     *
     * @param mangas the list of manga.
     * @param amount the amount to queue or null to queue all
     */
    private fun downloadUnreadChapters(mangas: List<Manga>, amount: Int?) {
        coroutineScope.launchNonCancellable {
            mangas.forEach { manga ->
                val chapters = getNextChapters.await(manga.id)
                    .fastFilterNot { chapter ->
                        downloadManager.getQueuedDownloadOrNull(chapter.id) != null ||
                            downloadManager.isChapterDownloaded(
                                chapter.name,
                                chapter.scanlator,
                                manga.title,
                                manga.source,
                            )
                    }
                    .let { if (amount != null) it.take(amount) else it }

                downloadManager.downloadChapters(manga, chapters)
            }
        }
    }

    /**
     * Marks mangas' chapters read status.
     */
    fun markReadSelection(read: Boolean) {
        val mangas = state.value.selection.toList()
        coroutineScope.launchNonCancellable {
            mangas.forEach { manga ->
                setReadStatus.await(
                    manga = manga.manga,
                    read = read,
                )
            }
        }
        clearSelection()
    }

    /**
     * Remove the selected manga.
     *
     * @param mangaList the list of manga to delete.
     * @param deleteFromLibrary whether to delete manga from library.
     * @param deleteChapters whether to delete downloaded chapters.
     */
    fun removeMangas(mangaList: List<Manga>, deleteFromLibrary: Boolean, deleteChapters: Boolean) {
        coroutineScope.launchNonCancellable {
            val mangaToDelete = mangaList.distinctBy { it.id }

            if (deleteFromLibrary) {
                val toDelete = mangaToDelete.map {
                    it.removeCovers(coverCache)
                    MangaUpdate(
                        favorite = false,
                        id = it.id,
                    )
                }
                updateManga.awaitAll(toDelete)
            }

            if (deleteChapters) {
                mangaToDelete.forEach { manga ->
                    val source = sourceManager.get(manga.source) as? HttpSource
                    if (source != null) {
                        downloadManager.deleteManga(manga, source)
                    }
                }
            }
        }
    }

    /**
     * Bulk update categories of manga using old and new common categories.
     *
     * @param mangaList the list of manga to move.
     * @param addCategories the categories to add for all mangas.
     * @param removeCategories the categories to remove in all mangas.
     */
    fun setMangaCategories(mangaList: List<Manga>, addCategories: List<Long>, removeCategories: List<Long>) {
        coroutineScope.launchNonCancellable {
            mangaList.forEach { manga ->
                val categoryIds = getCategories.await(manga.id)
                    .map { it.id }
                    .subtract(removeCategories.toSet())
                    .plus(addCategories)
                    .toList()

                setMangaCategories.await(manga.id, categoryIds)
            }
        }
    }

    fun getColumnsPreferenceForCurrentOrientation(isLandscape: Boolean): PreferenceMutableState<Int> {
        return (if (isLandscape) libraryPreferences.mangaLandscapeColumns() else libraryPreferences.mangaPortraitColumns()).asState(coroutineScope)
    }

    suspend fun getRandomLibraryItemForCurrentCategory(): MangaLibraryItem? {
        return withIOContext {
            state.value
                .getLibraryItemsByCategoryId(state.value.categories[activeCategoryIndex].id)
                ?.randomOrNull()
        }
    }

    fun showSettingsDialog() {
        mutableState.update { it.copy(dialog = Dialog.SettingsSheet) }
    }

    fun clearSelection() {
        mutableState.update { it.copy(selection = emptyList()) }
    }

    fun toggleSelection(manga: LibraryManga) {
        mutableState.update { state ->
            val newSelection = state.selection.toMutableList().apply {
                if (fastAny { it.id == manga.id }) {
                    removeAll { it.id == manga.id }
                } else {
                    add(manga)
                }
            }
            state.copy(selection = newSelection)
        }
    }

    /**
     * Selects all mangas between and including the given manga and the last pressed manga from the
     * same category as the given manga
     */
    fun toggleRangeSelection(manga: LibraryManga) {
        mutableState.update { state ->
            val newSelection = state.selection.toMutableList().apply {
                val lastSelected = lastOrNull()
                if (lastSelected?.category != manga.category) {
                    add(manga)
                    return@apply
                }

                val items = state.getLibraryItemsByCategoryId(manga.category)
                    ?.fastMap { it.libraryManga }.orEmpty()
                val lastMangaIndex = items.indexOf(lastSelected)
                val curMangaIndex = items.indexOf(manga)

                val selectedIds = fastMap { it.id }
                val selectionRange = when {
                    lastMangaIndex < curMangaIndex -> IntRange(lastMangaIndex, curMangaIndex)
                    curMangaIndex < lastMangaIndex -> IntRange(curMangaIndex, lastMangaIndex)
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
                state.getLibraryItemsByCategoryId(categoryId)
                    ?.fastMapNotNull { item ->
                        item.libraryManga.takeUnless { it.id in selectedIds }
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
                val items = state.getLibraryItemsByCategoryId(categoryId)?.fastMap { it.libraryManga }.orEmpty()
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
            // Create a copy of selected manga
            val mangaList = state.value.selection.map { it.manga }

            // Hide the default category because it has a different behavior than the ones from db.
            val categories = state.value.categories.filter { it.id != 0L }

            // Get indexes of the common categories to preselect.
            val common = getCommonCategories(mangaList)
            // Get indexes of the mix categories to preselect.
            val mix = getMixCategories(mangaList)
            val preselected = categories.map {
                when (it) {
                    in common -> CheckboxState.State.Checked(it)
                    in mix -> CheckboxState.TriState.Exclude(it)
                    else -> CheckboxState.State.None(it)
                }
            }
            mutableState.update { it.copy(dialog = Dialog.ChangeCategory(mangaList, preselected)) }
        }
    }

    fun openDeleteMangaDialog() {
        val mangaList = state.value.selection.map { it.manga }
        mutableState.update { it.copy(dialog = Dialog.DeleteManga(mangaList)) }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    sealed class Dialog {
        object SettingsSheet : Dialog()
        data class ChangeCategory(val manga: List<Manga>, val initialSelection: List<CheckboxState<Category>>) : Dialog()
        data class DeleteManga(val manga: List<Manga>) : Dialog()
    }

    @Immutable
    private data class ItemPreferences(
        val downloadBadge: Boolean,
        val localBadge: Boolean,
        val languageBadge: Boolean,

        val globalFilterDownloaded: Boolean,
        val filterDownloaded: TriStateFilter,
        val filterUnread: TriStateFilter,
        val filterStarted: TriStateFilter,
        val filterBookmarked: TriStateFilter,
        val filterCompleted: TriStateFilter,
    )

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val library: MangaLibraryMap = emptyMap(),
        val searchQuery: String? = null,
        val selection: List<LibraryManga> = emptyList(),
        val hasActiveFilters: Boolean = false,
        val showCategoryTabs: Boolean = false,
        val showMangaCount: Boolean = false,
        val showMangaContinueButton: Boolean = false,
        val dialog: Dialog? = null,
    ) {
        private val libraryCount by lazy {
            library.values
                .flatten()
                .fastDistinctBy { it.libraryManga.manga.id }
                .size
        }

        val isLibraryEmpty by lazy { libraryCount == 0 }

        val selectionMode = selection.isNotEmpty()

        val categories = library.keys.toList()

        fun getLibraryItemsByCategoryId(categoryId: Long): List<MangaLibraryItem>? {
            return library.firstNotNullOfOrNull { (k, v) -> v.takeIf { k.id == categoryId } }
        }

        fun getLibraryItemsByPage(page: Int): List<MangaLibraryItem> {
            return library.values.toTypedArray().getOrNull(page).orEmpty()
        }

        fun getMangaCountForCategory(category: Category): Int? {
            return if (showMangaCount || !searchQuery.isNullOrEmpty()) library[category]?.size else null
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
                !showMangaCount -> null
                !showCategoryTabs -> getMangaCountForCategory(category)
                // Whole library count
                else -> libraryCount
            }

            return LibraryToolbarTitle(title, count)
        }
    }
}
