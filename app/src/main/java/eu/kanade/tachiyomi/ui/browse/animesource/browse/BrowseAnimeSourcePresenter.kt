package eu.kanade.tachiyomi.ui.browse.animesource.browse

import android.os.Bundle
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.domain.anime.interactor.GetAnime
import eu.kanade.domain.anime.interactor.GetDuplicateLibraryAnime
import eu.kanade.domain.anime.interactor.InsertAnime
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.toAnimeUpdate
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.animetrack.interactor.InsertAnimeTrack
import eu.kanade.domain.animetrack.model.toDomainTrack
import eu.kanade.domain.category.interactor.GetCategoriesAnime
import eu.kanade.domain.category.interactor.SetAnimeCategories
import eu.kanade.domain.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.episode.interactor.SyncEpisodesWithTrackServiceTwoWay
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.toSAnime
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.toAnimeInfo
import eu.kanade.tachiyomi.data.database.models.toDomainAnime
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.EnhancedTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.browse.animesource.filter.CheckboxItem
import eu.kanade.tachiyomi.ui.browse.animesource.filter.CheckboxSectionItem
import eu.kanade.tachiyomi.ui.browse.animesource.filter.GroupItem
import eu.kanade.tachiyomi.ui.browse.animesource.filter.HeaderItem
import eu.kanade.tachiyomi.ui.browse.animesource.filter.SelectItem
import eu.kanade.tachiyomi.ui.browse.animesource.filter.SelectSectionItem
import eu.kanade.tachiyomi.ui.browse.animesource.filter.SeparatorItem
import eu.kanade.tachiyomi.ui.browse.animesource.filter.SortGroup
import eu.kanade.tachiyomi.ui.browse.animesource.filter.SortItem
import eu.kanade.tachiyomi.ui.browse.animesource.filter.TextItem
import eu.kanade.tachiyomi.ui.browse.animesource.filter.TextSectionItem
import eu.kanade.tachiyomi.ui.browse.animesource.filter.TriStateItem
import eu.kanade.tachiyomi.ui.browse.animesource.filter.TriStateSectionItem
import eu.kanade.tachiyomi.util.episode.EpisodeSettingsHelper
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import eu.kanade.domain.anime.model.Anime as DomainAnime
import eu.kanade.domain.category.model.Category as DomainCategory

open class BrowseAnimeSourcePresenter(
    private val sourceId: Long,
    searchQuery: String? = null,
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val prefs: PreferencesHelper = Injekt.get(),
    private val coverCache: AnimeCoverCache = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val getDuplicateLibraryAnime: GetDuplicateLibraryAnime = Injekt.get(),
    private val getCategories: GetCategoriesAnime = Injekt.get(),
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    private val insertAnime: InsertAnime = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val insertTrack: InsertAnimeTrack = Injekt.get(),
    private val syncEpisodesWithTrackServiceTwoWay: SyncEpisodesWithTrackServiceTwoWay = Injekt.get(),
) : BasePresenter<BrowseAnimeSourceController>() {

    /**
     * Selected source.
     */
    lateinit var source: AnimeCatalogueSource

    /**
     * Modifiable list of filters.
     */
    var sourceFilters = AnimeFilterList()
        set(value) {
            field = value
            filterItems = value.toItems()
        }

    var filterItems: List<IFlexible<*>> = emptyList()

    /**
     * List of filters used by the [Pager]. If empty alongside [query], the popular query is used.
     */
    var appliedFilters = AnimeFilterList()

    /**
     * Pager containing a list of anime results.
     */
    private lateinit var pager: AnimePager

    /**
     * Subscription for the pager.
     */
    private var pagerSubscription: Subscription? = null

    /**
     * Subscription for one request from the pager.
     */
    private var nextPageJob: Job? = null

    private val loggedServices by lazy { Injekt.get<TrackManager>().services.filter { it.isLogged } }

    init {
        query = searchQuery ?: ""
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        source = sourceManager.get(sourceId) as? AnimeCatalogueSource ?: return

        sourceFilters = source.getFilterList()

        if (savedState != null) {
            query = savedState.getString(::query.name, "")
        }

        restartPager()
    }

    override fun onSave(state: Bundle) {
        state.putString(::query.name, query)
        super.onSave(state)
    }

    /**
     * Restarts the pager for the active source with the provided query and filters.
     *
     * @param query the query.
     * @param filters the current state of the filters (for search mode).
     */
    fun restartPager(query: String = this.query, filters: AnimeFilterList = this.appliedFilters) {
        this.query = query
        this.appliedFilters = filters

        // Create a new pager.
        pager = createPager(query, filters)

        val sourceId = source.id

        val sourceDisplayMode = prefs.sourceDisplayMode()

        // Prepare the pager.
        pagerSubscription?.let { remove(it) }
        pagerSubscription = pager.results()
            .observeOn(Schedulers.io())
            .map { (first, second) -> first to second.map { networkToLocalAnime(it, sourceId).toDomainAnime()!! } }
            .doOnNext { initializeAnimes(it.second) }
            .map { (first, second) -> first to second.map { AnimeSourceItem(it, sourceDisplayMode) } }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeReplay(
                { view, (page, animes) ->
                    view.onAddPage(page, animes)
                },
                { _, error ->
                    logcat(LogPriority.ERROR, error)
                },
            )

        // Request first page.
        requestNext()
    }

    /**
     * Requests the next page for the active pager.
     */
    fun requestNext() {
        if (!hasNextPage()) return

        nextPageJob?.cancel()
        nextPageJob = launchIO {
            try {
                pager.requestNextPage()
            } catch (e: Throwable) {
                withUIContext { view?.onAddPageError(e) }
            }
        }
    }

    /**
     * Returns true if the last fetched page has a next page.
     */
    fun hasNextPage(): Boolean {
        return pager.hasNextPage
    }

    /**
     * Returns a anime from the database for the given anime from network. It creates a new entry
     * if the anime is not yet in the database.
     *
     * @param sAnime the anime from the source.
     * @return a anime from the database.
     */
    private fun networkToLocalAnime(sAnime: SAnime, sourceId: Long): Anime {
        var localAnime = runBlocking { getAnime.await(sAnime.url, sourceId) }
        if (localAnime == null) {
            val newAnime = Anime.create(sAnime.url, sAnime.title, sourceId)
            newAnime.copyFrom(sAnime)
            newAnime.id = -1
            val result = runBlocking {
                val id = insertAnime.await(newAnime.toDomainAnime()!!)
                getAnime.await(id!!)
            }
            localAnime = result
        } else if (!localAnime.favorite) {
            // if the anime isn't a favorite, set its display title from source
            // if it later becomes a favorite, updated title will go to db
            localAnime = localAnime.copy(title = sAnime.title)
        }
        return localAnime?.toDbAnime()!!
    }

    /**
     * Initialize a list of anime.
     *
     * @param animes the list of anime to initialize.
     */
    fun initializeAnimes(animes: List<DomainAnime>) {
        presenterScope.launchIO {
            animes.asFlow()
                .filter { it.thumbnailUrl == null && !it.initialized }
                .map { getAnimeDetails(it.toDbAnime()) }
                .onEach {
                    withUIContext {
                        @Suppress("DEPRECATION")
                        view?.onAnimeInitialized(it.toDomainAnime()!!)
                    }
                }
                .catch { e -> logcat(LogPriority.ERROR, e) }
                .collect()
        }
    }

    /**
     * Returns the initialized anime.
     *
     * @param anime the anime to initialize.
     * @return the initialized anime
     */
    private suspend fun getAnimeDetails(anime: Anime): Anime {
        try {
            val networkAnime = source.getAnimeDetails(anime.toAnimeInfo())
            anime.copyFrom(networkAnime.toSAnime())
            anime.initialized = true
            updateAnime.await(
                anime
                    .toDomainAnime()
                    ?.toAnimeUpdate()!!,
            )
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
        return anime
    }

    /**
     * Adds or removes a anime from the library.
     *
     * @param anime the anime to update.
     */
    fun changeAnimeFavorite(anime: Anime) {
        anime.favorite = !anime.favorite
        anime.date_added = when (anime.favorite) {
            true -> Date().time
            false -> 0
        }

        if (!anime.favorite) {
            anime.removeCovers(coverCache)
        } else {
            EpisodeSettingsHelper.applySettingDefaults(anime.toDomainAnime()!!)

            autoAddTrack(anime)
        }

        runBlocking {
            updateAnime.await(
                anime
                    .toDomainAnime()
                    ?.toAnimeUpdate()!!,
            )
        }
    }

    private fun autoAddTrack(anime: Anime) {
        launchIO {
            loggedServices
                .filterIsInstance<EnhancedTrackService>()
                .filter { it.accept(source) }
                .forEach { service ->
                    try {
                        service.match(anime)?.let { track ->
                            track.anime_id = anime.id!!
                            (service as TrackService).bind(track)
                            insertTrack.await(track.toDomainTrack()!!)

                            val episodes = getEpisodeByAnimeId.await(anime.id!!)
                            syncEpisodesWithTrackServiceTwoWay.await(episodes, track.toDomainTrack()!!, service)
                        }
                    } catch (e: Exception) {
                        logcat(LogPriority.WARN, e) { "Could not match anime: ${anime.title} with service $service" }
                    }
                }
        }
    }

    /**
     * Set the filter states for the current source.
     *
     * @param filters a list of active filters.
     */
    fun setSourceFilter(filters: AnimeFilterList) {
        restartPager(filters = filters)
    }

    open fun createPager(query: String, filters: AnimeFilterList): AnimePager {
        return AnimeSourcePager(source, query, filters)
    }

    private fun AnimeFilterList.toItems(): List<IFlexible<*>> {
        return mapNotNull { filter ->
            when (filter) {
                is AnimeFilter.Header -> HeaderItem(filter)
                is AnimeFilter.Separator -> SeparatorItem(filter)
                is AnimeFilter.CheckBox -> CheckboxItem(filter)
                is AnimeFilter.TriState -> TriStateItem(filter)
                is AnimeFilter.Text -> TextItem(filter)
                is AnimeFilter.Select<*> -> SelectItem(filter)
                is AnimeFilter.Group<*> -> {
                    val group = GroupItem(filter)
                    val subItems = filter.state.mapNotNull {
                        when (it) {
                            is AnimeFilter.CheckBox -> CheckboxSectionItem(it)
                            is AnimeFilter.TriState -> TriStateSectionItem(it)
                            is AnimeFilter.Text -> TextSectionItem(it)
                            is AnimeFilter.Select<*> -> SelectSectionItem(it)
                            else -> null
                        }
                    }
                    subItems.forEach { it.header = group }
                    group.subItems = subItems
                    group
                }
                is AnimeFilter.Sort -> {
                    val group = SortGroup(filter)
                    val subItems = filter.values.map {
                        SortItem(it, group)
                    }
                    group.subItems = subItems
                    group
                }
            }
        }
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<DomainCategory> {
        return getCategories.subscribe().firstOrNull() ?: emptyList()
    }

    suspend fun getDuplicateLibraryAnime(anime: DomainAnime): DomainAnime? {
        return getDuplicateLibraryAnime.await(anime.title, anime.source)
    }

    /**
     * Gets the category id's the anime is in, if the anime is not in a category, returns the default id.
     *
     * @param anime the anime to get categories from.
     * @return Array of category ids the anime is in, if none returns default id
     */
    fun getAnimeCategoryIds(anime: DomainAnime): Array<Long?> {
        return runBlocking { getCategories.await(anime.id) }
            .map { it.id }
            .toTypedArray()
    }

    /**
     * Move the given anime to categories.
     *
     * @param categories the selected categories.
     * @param anime the anime to move.
     */
    private fun moveAnimeToCategories(anime: Anime, categories: List<DomainCategory>) {
        presenterScope.launchIO {
            setAnimeCategories.await(
                animeId = anime.id!!,
                categoryIds = categories.filter { it.id != 0L }.map { it.id },
            )
        }
    }

    /**
     * Move the given anime to the category.
     *
     * @param category the selected category.
     * @param anime the anime to move.
     */
    fun moveAnimeToCategory(anime: Anime, category: DomainCategory?) {
        moveAnimeToCategories(anime, listOfNotNull(category))
    }

    /**
     * Update anime to use selected categories.
     *
     * @param anime needed to change
     * @param selectedCategories selected categories
     */
    fun updateAnimeCategories(anime: Anime, selectedCategories: List<DomainCategory>) {
        if (!anime.favorite) {
            changeAnimeFavorite(anime)
        }

        moveAnimeToCategories(anime, selectedCategories)
    }
}
