package eu.kanade.tachiyomi.ui.animelib

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import eu.kanade.core.prefs.CheckboxState
import eu.kanade.core.prefs.PreferenceMutableState
import eu.kanade.domain.anime.interactor.GetAnimelibAnime
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.AnimeUpdate
import eu.kanade.domain.anime.model.isLocal
import eu.kanade.domain.animehistory.interactor.GetNextEpisodes
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.domain.animetrack.interactor.GetTracksPerAnime
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.category.interactor.GetAnimeCategories
import eu.kanade.domain.category.interactor.SetAnimeCategories
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.episode.interactor.SetSeenStatus
import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.domain.library.model.LibrarySort
import eu.kanade.domain.library.model.sort
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.presentation.animelib.AnimelibState
import eu.kanade.presentation.animelib.AnimelibStateImpl
import eu.kanade.presentation.animelib.components.AnimelibToolbarTitle
import eu.kanade.presentation.category.visualName
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadCache
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadManager
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.models.toDomainAnime
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.episode.getNextUnseen
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.State
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.Collator
import java.util.Collections
import java.util.Locale
import eu.kanade.tachiyomi.data.database.models.Anime as DbAnime

/**
 * Class containing animelib information.
 */
private data class Animelib(val categories: List<Category>, val animeMap: AnimelibMap)

/**
 * Typealias for the animelib anime, using the category as keys, and list of anime as values.
 */
typealias AnimelibMap = Map<Long, List<AnimelibItem>>

/**
 * Presenter of [AnimelibController].
 */
class AnimelibPresenter(
    private val state: AnimelibStateImpl = AnimelibState() as AnimelibStateImpl,
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
) : BasePresenter<AnimelibController>(), AnimelibState by state {

    private var loadedAnime by mutableStateOf(emptyMap<Long, List<AnimelibItem>>())

    val isLibraryEmpty by derivedStateOf { loadedAnime.isEmpty() }

    val tabVisibility by libraryPreferences.categoryTabs().asState()
    val animeCountVisibility by libraryPreferences.categoryNumberOfItems().asState()

    val showDownloadBadges by libraryPreferences.downloadBadge().asState()
    val showUnseenBadges by libraryPreferences.unreadBadge().asState()
    val showLocalBadges by libraryPreferences.localBadge().asState()
    val showLanguageBadges by libraryPreferences.languageBadge().asState()

    var activeCategory: Int by libraryPreferences.lastUsedAnimeCategory().asState()

    val showContinueWatchingButton by libraryPreferences.showContinueReadingButton().asState()

    val isDownloadOnly: Boolean by preferences.downloadedOnly().asState()
    val isIncognitoMode: Boolean by preferences.incognitoMode().asState()

    private val _filterChanges: Channel<Unit> = Channel(Int.MAX_VALUE)
    private val filterChanges = _filterChanges.receiveAsFlow().onStart { emit(Unit) }

    private var animelibSubscription: Job? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        subscribeAnimelib()
    }

    /**
     * Subscribes to animelib if needed.
     */
    fun subscribeAnimelib() {
        /**
         * TODO:
         * - Move filter and sort to getAnimeForCategory and only filter and sort the current display category instead of whole animelib as some has 5000+ items in the animelib
         * - Create new db view and new query to just fetch the current category save as needed to instance variable
         * - Fetch badges to maps and retrieve as needed instead of fetching all of them at once
         */
        if (animelibSubscription == null || animelibSubscription!!.isCancelled) {
            animelibSubscription = presenterScope.launchIO {
                combine(getAnimelibFlow(), getTracksPerAnime.subscribe(), filterChanges) { animelib, tracks, _ ->
                    animelib.animeMap
                        .applyFilters(tracks)
                        .applySort(animelib.categories)
                }
                    .collectLatest {
                        state.isLoading = false
                        loadedAnime = it
                    }
            }
        }
    }

    /**
     * Applies animelib filters to the given map of anime.
     */
    private fun AnimelibMap.applyFilters(trackMap: Map<Long, List<Long>>): AnimelibMap {
        val downloadedOnly = preferences.downloadedOnly().get()
        val filterDownloaded = libraryPreferences.filterDownloaded().get()
        val filterUnseen = libraryPreferences.filterUnread().get()
        val filterStarted = libraryPreferences.filterStarted().get()
        val filterBookmarked = libraryPreferences.filterBookmarked().get()
        val filterCompleted = libraryPreferences.filterCompleted().get()
        val loggedInTrackServices = trackManager.services.filter { trackService -> trackService.isLogged }
            .associate { trackService ->
                trackService.id to libraryPreferences.filterTracking(trackService.id.toInt()).get()
            }
        val isNotLoggedInAnyTrack = loggedInTrackServices.isEmpty()

        val excludedTracks = loggedInTrackServices.mapNotNull { if (it.value == State.EXCLUDE.value) it.key else null }
        val includedTracks = loggedInTrackServices.mapNotNull { if (it.value == State.INCLUDE.value) it.key else null }
        val trackFiltersIsIgnored = includedTracks.isEmpty() && excludedTracks.isEmpty()

        val filterFnDownloaded: (AnimelibItem) -> Boolean = downloaded@{ item ->
            if (!downloadedOnly && filterDownloaded == State.IGNORE.value) return@downloaded true
            val isDownloaded = when {
                item.animelibAnime.anime.isLocal() -> true
                item.downloadCount != -1L -> item.downloadCount > 0
                else -> downloadManager.getDownloadCount(item.animelibAnime.anime) > 0
            }

            return@downloaded if (downloadedOnly || filterDownloaded == State.INCLUDE.value) {
                isDownloaded
            } else {
                !isDownloaded
            }
        }

        val filterFnUnseen: (AnimelibItem) -> Boolean = unseen@{ item ->
            if (filterUnseen == State.IGNORE.value) return@unseen true
            val isUnseen = item.animelibAnime.unseenCount > 0

            return@unseen if (filterUnseen == State.INCLUDE.value) {
                isUnseen
            } else {
                !isUnseen
            }
        }

        val filterFnStarted: (AnimelibItem) -> Boolean = started@{ item ->
            if (filterStarted == State.IGNORE.value) return@started true
            val hasStarted = item.animelibAnime.hasStarted

            return@started if (filterStarted == State.INCLUDE.value) {
                hasStarted
            } else {
                !hasStarted
            }
        }

        val filterFnBookmarked: (AnimelibItem) -> Boolean = bookmarked@{ item ->
            if (filterBookmarked == State.IGNORE.value) return@bookmarked true

            val hasBookmarks = item.animelibAnime.hasBookmarks

            return@bookmarked if (filterBookmarked == State.INCLUDE.value) {
                hasBookmarks
            } else {
                !hasBookmarks
            }
        }

        val filterFnCompleted: (AnimelibItem) -> Boolean = completed@{ item ->
            if (filterCompleted == State.IGNORE.value) return@completed true
            val isCompleted = item.animelibAnime.anime.status.toInt() == SAnime.COMPLETED

            return@completed if (filterCompleted == State.INCLUDE.value) {
                isCompleted
            } else {
                !isCompleted
            }
        }

        val filterFnTracking: (AnimelibItem) -> Boolean = tracking@{ item ->
            if (isNotLoggedInAnyTrack || trackFiltersIsIgnored) return@tracking true

            val animeTracks = trackMap[item.animelibAnime.id].orEmpty()

            val exclude = animeTracks.filter { it in excludedTracks }
            val include = animeTracks.filter { it in includedTracks }

            // TODO: Simplify the filter logic
            if (includedTracks.isNotEmpty() && excludedTracks.isNotEmpty()) {
                return@tracking if (exclude.isNotEmpty()) false else include.isNotEmpty()
            }

            if (excludedTracks.isNotEmpty()) return@tracking exclude.isEmpty()

            if (includedTracks.isNotEmpty()) return@tracking include.isNotEmpty()

            return@tracking false
        }

        val filterFn: (AnimelibItem) -> Boolean = filter@{ item ->
            return@filter !(
                !filterFnDownloaded(item) ||
                    !filterFnUnseen(item) ||
                    !filterFnStarted(item) ||
                    !filterFnBookmarked(item) ||
                    !filterFnCompleted(item) ||
                    !filterFnTracking(item)
                )
        }

        return this.mapValues { entry -> entry.value.filter(filterFn) }
    }

    /**
     * Applies animelib sorting to the given map of anime.
     */
    private fun AnimelibMap.applySort(categories: List<Category>): AnimelibMap {
        val sortModes = categories.associate { it.id to it.sort }

        val locale = Locale.getDefault()
        val collator = Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
        val sortAlphabetically: (AnimelibItem, AnimelibItem) -> Int = { i1, i2 ->
            collator.compare(i1.animelibAnime.anime.title.lowercase(locale), i2.animelibAnime.anime.title.lowercase(locale))
        }

        val sortFn: (AnimelibItem, AnimelibItem) -> Int = { i1, i2 ->
            val sort = sortModes[i1.animelibAnime.category]!!
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
                else -> throw IllegalStateException("Invalid SortModeSetting: ${sort.type}")
            }
        }

        return this.mapValues { entry ->
            val comparator = if (sortModes[entry.key]!!.isAscending) {
                Comparator(sortFn)
            } else {
                Collections.reverseOrder(sortFn)
            }

            entry.value.sortedWith(comparator.thenComparator(sortAlphabetically))
        }
    }

    /**
     * Get the categories and all its anime from the database.
     *
     * @return an observable of the categories and its anime.
     */
    private fun getAnimelibFlow(): Flow<Animelib> {
        val animelibAnimesFlow = combine(
            getAnimelibAnime.subscribe(),
            libraryPreferences.downloadBadge().changes(),
            libraryPreferences.filterDownloaded().changes(),
            preferences.downloadedOnly().changes(),
            downloadCache.changes,
        ) { libraryAnimeList, downloadBadgePref, filterDownloadedPref, downloadedOnly, _ ->
            libraryAnimeList
                .map { animelibAnime ->
                    val needsDownloadCounts = downloadBadgePref ||
                        filterDownloadedPref != State.IGNORE.value ||
                        downloadedOnly

                    // Display mode based on user preference: take it from global animelib setting or category
                    AnimelibItem(animelibAnime).apply {
                        downloadCount = if (needsDownloadCounts) {
                            downloadManager.getDownloadCount(animelibAnime.anime).toLong()
                        } else {
                            0
                        }
                        unseenCount = animelibAnime.unseenCount
                        isLocal = animelibAnime.anime.isLocal()
                        sourceLanguage = sourceManager.getOrStub(animelibAnime.anime.source).lang
                    }
                }
                .groupBy { it.animelibAnime.category }
        }
        return combine(getCategories.subscribe(), animelibAnimesFlow) { categories, animelibAnime ->
            val displayCategories =
                if (animelibAnime.isNotEmpty() && animelibAnime.containsKey(0).not()) {
                    categories.filterNot { it.isSystemCategory }
                } else {
                    categories
                }
            state.categories = displayCategories
            Animelib(categories, animelibAnime)
        }
    }

    /**
     * Requests the animelib to be filtered.
     */
    suspend fun requestFilterUpdate() = withIOContext {
        _filterChanges.send(Unit)
    }

    /**
     * Called when an anime is opened.
     */
    fun onOpenAnime() {
        // Avoid further db updates for the animelib when it's not needed
        animelibSubscription?.cancel()
    }

    /**
     * Returns the common categories for the given list of anime.
     *
     * @param animes the list of anime.
     */
    suspend fun getCommonCategories(animes: List<Anime>): Collection<Category> {
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
    suspend fun getMixCategories(animes: List<Anime>): Collection<Category> {
        if (animes.isEmpty()) return emptyList()
        val animeCategories = animes.map { getCategories.await(it.id).toSet() }
        val common = animeCategories.reduce { set1, set2 -> set1.intersect(set2) }
        return animeCategories.flatten().distinct().subtract(common)
    }

    /**
     * Queues the amount specified of unseen episodes from the list of animes given.
     *
     * @param animes the list of anime.
     * @param amount the amount to queue or null to queue all
     */
    fun downloadUnseenEpisodes(animes: List<Anime>, amount: Int?) {
        presenterScope.launchNonCancellable {
            animes.forEach { anime ->
                val episodes = getNextEpisodes.await(anime.id)
                    .filterNot { episode ->
                        downloadManager.queue.any { episode.id == it.episode.id } ||
                            downloadManager.isEpisodeDownloaded(
                                episode.name,
                                episode.scanlator,
                                anime.title,
                                anime.source,
                            )
                    }
                    .let { if (amount != null) it.take(amount) else it }

                downloadManager.downloadEpisodes(anime, episodes.map { it.toDbEpisode() })
            }
        }
    }

    /**
     * Marks animes' episodes seen status.
     *
     * @param animes the list of anime.
     */
    fun markSeenStatus(animes: List<Anime>, seen: Boolean) {
        presenterScope.launchNonCancellable {
            animes.forEach { anime ->
                setSeenStatus.await(
                    anime = anime,
                    seen = seen,
                )
            }
        }
    }

    /**
     * Remove the selected anime.
     *
     * @param animeList the list of anime to delete.
     * @param deleteFromAnimelib whether to delete anime from animelib.
     * @param deleteEpisodes whether to delete downloaded episodes.
     */
    fun removeAnimes(animeList: List<DbAnime>, deleteFromAnimelib: Boolean, deleteEpisodes: Boolean) {
        presenterScope.launchNonCancellable {
            val animeToDelete = animeList.distinctBy { it.id }

            if (deleteFromAnimelib) {
                val toDelete = animeToDelete.map {
                    it.removeCovers(coverCache)
                    AnimeUpdate(
                        favorite = false,
                        id = it.id!!,
                    )
                }
                updateAnime.awaitAll(toDelete)
            }

            if (deleteEpisodes) {
                animeToDelete.forEach { anime ->
                    val source = sourceManager.get(anime.source) as? AnimeHttpSource
                    if (source != null) {
                        downloadManager.deleteAnime(anime.toDomainAnime()!!, source)
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
        presenterScope.launchNonCancellable {
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

    @Composable
    fun getAnimeCountForCategory(categoryId: Long): androidx.compose.runtime.State<Int?> {
        return produceState<Int?>(initialValue = null, loadedAnime) {
            value = loadedAnime[categoryId]?.size
        }
    }

    fun getColumnsPreferenceForCurrentOrientation(isLandscape: Boolean): PreferenceMutableState<Int> {
        return (if (isLandscape) libraryPreferences.landscapeColumns() else libraryPreferences.portraitColumns()).asState()
    }

    // TODO: This is good but should we separate title from count or get categories with count from db
    @Composable
    fun getToolbarTitle(): androidx.compose.runtime.State<AnimelibToolbarTitle> {
        val category = categories.getOrNull(activeCategory)

        val defaultTitle = if (libraryPreferences.bottomNavStyle().get() == 2) {
            stringResource(R.string.label_library)
        } else {
            stringResource(R.string.label_animelib)
        }
        val categoryName = category?.visualName ?: defaultTitle

        val default = remember { AnimelibToolbarTitle(defaultTitle) }

        return produceState(initialValue = default, category, loadedAnime, animeCountVisibility, tabVisibility) {
            val title = if (tabVisibility.not()) categoryName else defaultTitle
            val count = when {
                category == null || animeCountVisibility.not() -> null
                tabVisibility.not() -> loadedAnime[category.id]?.size
                else -> loadedAnime.values.flatten().distinctBy { it.animelibAnime.anime.id }.size
            }

            value = when (category) {
                null -> default
                else -> AnimelibToolbarTitle(title, count)
            }
        }
    }

    @Composable
    fun getAnimeForCategory(page: Int): List<AnimelibItem> {
        val unfiltered = remember(categories, loadedAnime, page) {
            val categoryId = categories.getOrNull(page)?.id ?: -1
            loadedAnime[categoryId] ?: emptyList()
        }
        return remember(unfiltered, searchQuery) {
            val query = searchQuery
            if (query.isNullOrBlank().not()) {
                unfiltered.filter {
                    it.filter(query!!)
                }
            } else {
                unfiltered
            }
        }
    }

    fun clearSelection() {
        state.selection = emptyList()
    }

    fun toggleSelection(anime: AnimelibAnime) {
        state.selection = selection.toMutableList().apply {
            if (fastAny { it.id == anime.id }) {
                removeAll { it.id == anime.id }
            } else {
                add(anime)
            }
        }
    }

    /**
     * Selects all animes between and including the given anime and the last pressed anime from the
     * same category as the given anime
     */
    fun toggleRangeSelection(anime: AnimelibAnime) {
        state.selection = selection.toMutableList().apply {
            val lastSelected = lastOrNull()
            if (lastSelected?.category != anime.category) {
                add(anime)
                return@apply
            }
            val items = loadedAnime[anime.category].orEmpty().fastMap { it.animelibAnime }
            val lastAnimeIndex = items.indexOf(lastSelected)
            val curAnimeIndex = items.indexOf(anime)
            val selectedIds = fastMap { it.id }
            val newSelections = when (lastAnimeIndex >= curAnimeIndex + 1) {
                true -> items.subList(curAnimeIndex, lastAnimeIndex)
                false -> items.subList(lastAnimeIndex, curAnimeIndex + 1)
            }.filterNot { it.id in selectedIds }
            addAll(newSelections)
        }
    }

    fun selectAll(index: Int) {
        state.selection = state.selection.toMutableList().apply {
            val categoryId = categories[index].id
            val items = loadedAnime[categoryId].orEmpty().fastMap { it.animelibAnime }
            val selectedIds = fastMap { it.id }
            val newSelections = items.filterNot { it.id in selectedIds }
            addAll(newSelections)
        }
    }

    fun invertSelection(index: Int) {
        state.selection = selection.toMutableList().apply {
            val categoryId = categories[index].id
            val items = loadedAnime[categoryId].orEmpty().fastMap { it.animelibAnime }
            val selectedIds = fastMap { it.id }
            val (toRemove, toAdd) = items.partition { it.id in selectedIds }
            val toRemoveIds = toRemove.fastMap { it.id }
            removeAll { it.id in toRemoveIds }
            addAll(toAdd)
        }
    }

    sealed class Dialog {
        data class ChangeCategory(val anime: List<Anime>, val initialSelection: List<CheckboxState<Category>>) : Dialog()
        data class DeleteAnime(val anime: List<Anime>) : Dialog()
        data class DownloadCustomAmount(val anime: List<Anime>, val max: Int) : Dialog()
    }
}
