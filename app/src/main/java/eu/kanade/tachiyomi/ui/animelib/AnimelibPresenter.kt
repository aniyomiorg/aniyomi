package eu.kanade.tachiyomi.ui.animelib

import android.os.Bundle
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeCategory
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.library.setting.SortDirectionSetting
import eu.kanade.tachiyomi.ui.library.setting.SortModeSetting
import eu.kanade.tachiyomi.util.isLocal
import eu.kanade.tachiyomi.util.lang.combineLatest
import eu.kanade.tachiyomi.util.lang.isNullOrUnsubscribed
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.State
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.Collator
import java.util.Collections
import java.util.Comparator
import java.util.Locale

/**
 * Class containing animelib information.
 */
private data class Animelib(val categories: List<Category>, val animeMap: AnimelibMap)

/**
 * Typealias for the animelib anime, using the category as keys, and list of anime as values.
 */
private typealias AnimelibMap = Map<Int, List<AnimelibItem>>

/**
 * Presenter of [AnimelibController].
 */
class AnimelibPresenter(
    private val db: AnimeDatabaseHelper = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
    private val coverCache: AnimeCoverCache = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val downloadManager: AnimeDownloadManager = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get()
) : BasePresenter<AnimelibController>() {

    private val context = preferences.context

    /**
     * Categories of the animelib.
     */
    var categories: List<Category> = emptyList()
        private set

    /**
     * Relay used to apply the UI filters to the last emission of the animelib.
     */
    private val filterTriggerRelay = BehaviorRelay.create(Unit)

    /**
     * Relay used to apply the UI update to the last emission of the animelib.
     */
    private val badgeTriggerRelay = BehaviorRelay.create(Unit)

    /**
     * Relay used to apply the selected sorting method to the last emission of the animelib.
     */
    private val sortTriggerRelay = BehaviorRelay.create(Unit)

    /**
     * Animelib subscription.
     */
    private var animelibSubscription: Subscription? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        subscribeAnimelib()
    }

    /**
     * Subscribes to animelib if needed.
     */
    fun subscribeAnimelib() {
        if (animelibSubscription.isNullOrUnsubscribed()) {
            animelibSubscription = getAnimelibObservable()
                .combineLatest(badgeTriggerRelay.observeOn(Schedulers.io())) { lib, _ ->
                    lib.apply { setBadges(animeMap) }
                }
                .combineLatest(getFilterObservable()) { lib, tracks ->
                    lib.copy(animeMap = applyFilters(lib.animeMap, tracks))
                }
                .combineLatest(sortTriggerRelay.observeOn(Schedulers.io())) { lib, _ ->
                    lib.copy(animeMap = applySort(lib.categories, lib.animeMap))
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache({ view, (categories, animeMap) ->
                    view.onNextAnimelibUpdate(categories, animeMap)
                })
        }
    }

    /**
     * Applies animelib filters to the given map of anime.
     *
     * @param map the map to filter.
     */
    private fun applyFilters(map: AnimelibMap, trackMap: Map<Long, Map<Int, Boolean>>): AnimelibMap {
        val downloadedOnly = preferences.downloadedOnly().get()
        val filterDownloaded = preferences.filterDownloaded().get()
        val filterUnread = preferences.filterUnread().get()
        val filterCompleted = preferences.filterCompleted().get()
        val loggedInServices = trackManager.services.filter { trackService -> trackService.isLogged }
            .associate { trackService ->
                Pair(trackService.id, preferences.filterTracking(trackService.id).get())
            }
        val isNotAnyLoggedIn = !loggedInServices.values.any()

        val filterFnUnread: (AnimelibItem) -> Boolean = unread@{ item ->
            if (filterUnread == State.IGNORE.value) return@unread true
            val isUnread = item.anime.unseen != 0

            return@unread if (filterUnread == State.INCLUDE.value) isUnread
            else !isUnread
        }

        val filterFnCompleted: (AnimelibItem) -> Boolean = completed@{ item ->
            if (filterCompleted == State.IGNORE.value) return@completed true
            val isCompleted = item.anime.status == SAnime.COMPLETED

            return@completed if (filterCompleted == State.INCLUDE.value) isCompleted
            else !isCompleted
        }

        val filterFnDownloaded: (AnimelibItem) -> Boolean = downloaded@{ item ->
            if (!downloadedOnly && filterDownloaded == State.IGNORE.value) return@downloaded true
            val isDownloaded = when {
                item.anime.isLocal() -> true
                item.downloadCount != -1 -> item.downloadCount > 0
                else -> downloadManager.getDownloadCount(item.anime) > 0
            }

            return@downloaded if (downloadedOnly || filterDownloaded == State.INCLUDE.value) isDownloaded
            else !isDownloaded
        }

        val filterFnTracking: (AnimelibItem) -> Boolean = tracking@{ item ->
            if (isNotAnyLoggedIn) return@tracking true

            val trackedAnime = trackMap[item.anime.id ?: -1]

            val containsExclude = loggedInServices.filterValues { it == State.EXCLUDE.value }
            val containsInclude = loggedInServices.filterValues { it == State.INCLUDE.value }

            if (!containsExclude.any() && !containsInclude.any()) return@tracking true

            val exclude = trackedAnime?.filterKeys { containsExclude.containsKey(it) }?.values ?: emptyList()
            val include = trackedAnime?.filterKeys { containsInclude.containsKey(it) }?.values ?: emptyList()

            if (containsInclude.any() && containsExclude.any()) {
                return@tracking if (exclude.isNotEmpty()) !exclude.any() else include.any()
            }

            if (containsExclude.any()) return@tracking !exclude.any()

            if (containsInclude.any()) return@tracking include.any()

            return@tracking false
        }

        val filterFn: (AnimelibItem) -> Boolean = filter@{ item ->
            return@filter !(
                !filterFnUnread(item) ||
                    !filterFnCompleted(item) ||
                    !filterFnDownloaded(item) ||
                    !filterFnTracking(item)
                )
        }

        return map.mapValues { entry -> entry.value.filter(filterFn) }
    }

    /**
     * Sets downloaded episode count to each anime.
     *
     * @param map the map of anime.
     */
    private fun setBadges(map: AnimelibMap) {
        val showDownloadBadges = preferences.downloadBadge().get()
        val showUnreadBadges = preferences.unreadBadge().get()
        val showLocalBadges = preferences.localBadge().get()
        val showLanguageBadges = preferences.languageBadge().get()

        for ((_, itemList) in map) {
            for (item in itemList) {
                item.downloadCount = if (showDownloadBadges) {
                    downloadManager.getDownloadCount(item.anime)
                } else {
                    // Unset download count if not enabled
                    -1
                }

                item.unreadCount = if (showUnreadBadges) {
                    item.anime.unseen
                } else {
                    // Unset unread count if not enabled
                    -1
                }

                item.isLocal = if (showLocalBadges) {
                    item.anime.isLocal()
                } else {
                    // Hide / Unset local badge if not enabled
                    false
                }

                item.sourceLanguage = if (showLanguageBadges) {
                    sourceManager.getOrStub(item.anime.source).lang.uppercase()
                } else {
                    // Unset source language if not enabled
                    ""
                }
            }
        }
    }

    /**
     * Applies animelib sorting to the given map of anime.
     *
     * @param map the map to sort.
     */
    private fun applySort(categories: List<Category>, map: AnimelibMap): AnimelibMap {
        val lastReadAnime by lazy {
            var counter = 0
            db.getLastSeenAnime().executeAsBlocking().associate { it.id!! to counter++ }
        }
        val totalEpisodeAnime by lazy {
            var counter = 0
            db.getTotalEpisodeAnime().executeAsBlocking().associate { it.id!! to counter++ }
        }
        val latestChapterAnime by lazy {
            var counter = 0
            db.getLatestEpisodeAnime().executeAsBlocking().associate { it.id!! to counter++ }
        }
        val chapterFetchDateAnime by lazy {
            var counter = 0
            db.getEpisodeFetchDateAnime().executeAsBlocking().associate { it.id!! to counter++ }
        }

        val sortingModes = categories.associate { category ->
            (category.id ?: 0) to SortModeSetting.get(preferences, category)
        }

        val sortAscending = categories.associate { category ->
            (category.id ?: 0) to SortDirectionSetting.get(preferences, category)
        }

        val locale = Locale.getDefault()
        val collator = Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
        val sortFn: (AnimelibItem, AnimelibItem) -> Int = { i1, i2 ->
            val sortingMode = sortingModes[i1.anime.category]!!
            val sortAscending = sortAscending[i1.anime.category]!! == SortDirectionSetting.ASCENDING
            when (sortingMode) {
                SortModeSetting.ALPHABETICAL -> {
                    collator.compare(i1.anime.title.lowercase(locale), i2.anime.title.lowercase(locale))
                }
                SortModeSetting.LAST_READ -> {
                    // Get index of anime, set equal to list if size unknown.
                    val anime1LastRead = lastReadAnime[i1.anime.id!!] ?: lastReadAnime.size
                    val anime2LastRead = lastReadAnime[i2.anime.id!!] ?: lastReadAnime.size
                    anime1LastRead.compareTo(anime2LastRead)
                }
                SortModeSetting.LAST_CHECKED -> i2.anime.last_update.compareTo(i1.anime.last_update)
                SortModeSetting.UNREAD -> when {
                    // Ensure unread content comes first
                    i1.anime.unseen == i2.anime.unseen -> 0
                    i1.anime.unseen == 0 -> if (sortAscending) 1 else -1
                    i2.anime.unseen == 0 -> if (sortAscending) -1 else 1
                    else -> i1.anime.unseen.compareTo(i2.anime.unseen)
                }
                SortModeSetting.TOTAL_CHAPTERS -> {
                    val anime1TotalEpisode = totalEpisodeAnime[i1.anime.id!!] ?: 0
                    val anime2TotalEpisode = totalEpisodeAnime[i2.anime.id!!] ?: 0
                    anime1TotalEpisode.compareTo(anime2TotalEpisode)
                }
                SortModeSetting.LATEST_CHAPTER -> {
                    val anime1latestEpisode = latestChapterAnime[i1.anime.id!!]
                        ?: latestChapterAnime.size
                    val anime2latestEpisode = latestChapterAnime[i2.anime.id!!]
                        ?: latestChapterAnime.size
                    anime1latestEpisode.compareTo(anime2latestEpisode)
                }
                SortModeSetting.DATE_FETCHED -> {
                    val anime1chapterFetchDate = chapterFetchDateAnime[i1.anime.id!!]
                        ?: chapterFetchDateAnime.size
                    val anime2chapterFetchDate = chapterFetchDateAnime[i2.anime.id!!]
                        ?: chapterFetchDateAnime.size
                    anime1chapterFetchDate.compareTo(anime2chapterFetchDate)
                }
                SortModeSetting.DATE_ADDED -> i2.anime.date_added.compareTo(i1.anime.date_added)
            }
        }

        return map.mapValues { entry ->
            val sortAscending = sortAscending[entry.key]!! == SortDirectionSetting.ASCENDING

            val comparator = if (sortAscending) {
                Comparator(sortFn)
            } else {
                Collections.reverseOrder(sortFn)
            }

            entry.value.sortedWith(comparator)
        }
    }

    /**
     * Get the categories and all its anime from the database.
     *
     * @return an observable of the categories and its anime.
     */
    private fun getAnimelibObservable(): Observable<Animelib> {
        return Observable.combineLatest(getCategoriesObservable(), getAnimelibAnimesObservable()) { dbCategories, animelibAnime ->
            val categories = if (animelibAnime.containsKey(0)) {
                arrayListOf(Category.createDefault(context)) + dbCategories
            } else {
                dbCategories
            }

            animelibAnime.forEach { (categoryId, animelibAnime) ->
                val category = categories.first { category -> category.id == categoryId }
                animelibAnime.forEach { libraryItem ->
                    libraryItem.displayMode = category.displayMode
                }
            }

            this.categories = categories
            Animelib(categories, animelibAnime)
        }
    }

    /**
     * Get the categories from the database.
     *
     * @return an observable of the categories.
     */
    private fun getCategoriesObservable(): Observable<List<Category>> {
        return db.getCategories().asRxObservable()
    }

    /**
     * Get the anime grouped by categories.
     *
     * @return an observable containing a map with the category id as key and a list of anime as the
     * value.
     */
    private fun getAnimelibAnimesObservable(): Observable<AnimelibMap> {
        val defaultLibraryDisplayMode = preferences.libraryDisplayMode()
        val shouldSetFromCategory = preferences.categorizedDisplaySettings()
        return db.getAnimelibAnimes().asRxObservable()
            .map { list ->
                list.map { animelibAnime ->
                    // Display mode based on user preference: take it from global library setting or category
                    AnimelibItem(
                        animelibAnime,
                        shouldSetFromCategory,
                        defaultLibraryDisplayMode
                    )
                }.groupBy { it.anime.category }
            }
    }

    /**
     * Get the tracked anime from the database and checks if the filter gets changed
     *
     * @return an observable of tracked anime.
     */
    private fun getFilterObservable(): Observable<Map<Long, Map<Int, Boolean>>> {
        return getTracksObservable().combineLatest(filterTriggerRelay.observeOn(Schedulers.io())) { tracks, _ -> tracks }
    }

    /**
     * Get the tracked anime from the database
     *
     * @return an observable of tracked anime.
     */
    private fun getTracksObservable(): Observable<Map<Long, Map<Int, Boolean>>> {
        return db.getTracks().asRxObservable().map { tracks ->
            tracks.groupBy { it.anime_id }
                .mapValues { tracksForAnimeId ->
                    // Check if any of the trackers is logged in for the current anime id
                    tracksForAnimeId.value.associate {
                        Pair(it.sync_id, trackManager.getService(it.sync_id)?.isLogged ?: false)
                    }
                }
        }.observeOn(Schedulers.io())
    }

    /**
     * Requests the animelib to be filtered.
     */
    fun requestFilterUpdate() {
        filterTriggerRelay.call(Unit)
    }

    /**
     * Requests the animelib to have download badges added.
     */
    fun requestBadgesUpdate() {
        badgeTriggerRelay.call(Unit)
    }

    /**
     * Requests the animelib to be sorted.
     */
    fun requestSortUpdate() {
        sortTriggerRelay.call(Unit)
    }

    /**
     * Called when a anime is opened.
     */
    fun onOpenAnime() {
        // Avoid further db updates for the animelib when it's not needed
        animelibSubscription?.let { remove(it) }
    }

    /**
     * Returns the common categories for the given list of anime.
     *
     * @param animes the list of anime.
     */
    fun getCommonCategories(animes: List<Anime>): Collection<Category> {
        if (animes.isEmpty()) return emptyList()
        return animes.toSet()
            .map { db.getCategoriesForAnime(it).executeAsBlocking() }
            .reduce { set1: Iterable<Category>, set2 -> set1.intersect(set2).toMutableList() }
    }

    /**
     * Returns the mix (non-common) categories for the given list of manga.
     *
     * @param animes the list of anime.
     */
    fun getMixCategories(animes: List<Anime>): Collection<Category> {
        if (animes.isEmpty()) return emptyList()
        val animeCategories = animes.toSet().map { db.getCategoriesForAnime(it).executeAsBlocking() }
        val common = animeCategories.reduce { set1, set2 -> set1.intersect(set2).toMutableList() }
        return animeCategories.flatten().distinct().subtract(common).toMutableList()
    }

    /**
     * Queues all unread episodes from the given list of anime.
     *
     * @param animes the list of anime.
     */
    fun downloadUnseenEpisodes(animes: List<Anime>) {
        animes.forEach { anime ->
            launchIO {
                val episodes = db.getEpisodes(anime).executeAsBlocking()
                    .filter { !it.seen }

                downloadManager.downloadEpisodes(anime, episodes)
            }
        }
    }

    /**
     * Marks animes' episodes seen status.
     *
     * @param animes the list of anime.
     */
    fun markSeenStatus(animes: List<Anime>, seen: Boolean) {
        animes.forEach { anime ->
            launchIO {
                val episodes = db.getEpisodes(anime).executeAsBlocking()
                episodes.forEach {
                    it.seen = seen
                    if (!seen) {
                        it.last_second_seen = 0
                    }
                }
                db.updateEpisodesProgress(episodes).executeAsBlocking()

                if (seen && preferences.removeAfterMarkedAsRead()) {
                    deleteEpisodes(anime, episodes)
                }
            }
        }
    }

    private fun deleteEpisodes(anime: Anime, episodes: List<Episode>) {
        sourceManager.get(anime.source)?.let { source ->
            downloadManager.deleteEpisodes(episodes, anime, source)
        }
    }

    /**
     * Remove the selected anime.
     *
     * @param animes the list of anime to delete.
     * @param deleteFromAnimelib whether to delete anime from animelib.
     * @param deleteEpisodes whether to delete downloaded episodes.
     */
    fun removeAnimes(animes: List<Anime>, deleteFromAnimelib: Boolean, deleteEpisodes: Boolean) {
        launchIO {
            val animeToDelete = animes.distinctBy { it.id }

            if (deleteFromAnimelib) {
                animeToDelete.forEach {
                    it.favorite = false
                    it.removeCovers(coverCache)
                }
                db.insertAnimes(animeToDelete).executeAsBlocking()
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
     * Move the given list of anime to categories.
     *
     * @param categories the selected categories.
     * @param animes the list of anime to move.
     */
    fun moveAnimesToCategories(categories: List<Category>, animes: List<Anime>) {
        val mc = mutableListOf<AnimeCategory>()

        for (anime in animes) {
            categories.mapTo(mc) { AnimeCategory.create(anime, it) }
        }

        db.setAnimeCategories(mc, animes)
    }

    /**
     * Bulk update categories of animes using old and new common categories.
     *
     * @param animes the list of anime to move.
     * @param addCategories the categories to add for all animes.
     * @param removeCategories the categories to remove in all animes.
     */
    fun updateAnimesToCategories(animes: List<Anime>, addCategories: List<Category>, removeCategories: List<Category>) {
        val animeCategories = animes.map { anime ->
            val categories = db.getCategoriesForAnime(anime).executeAsBlocking()
                .subtract(removeCategories).plus(addCategories).distinct()
            categories.map { AnimeCategory.create(anime, it) }
        }.flatten()

        db.setAnimeCategories(animeCategories, animes)
    }
}
