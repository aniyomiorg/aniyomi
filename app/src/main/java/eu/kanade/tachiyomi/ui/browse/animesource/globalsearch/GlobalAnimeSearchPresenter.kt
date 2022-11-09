package eu.kanade.tachiyomi.ui.browse.animesource.globalsearch

import android.os.Bundle
import eu.kanade.domain.anime.interactor.NetworkToLocalAnime
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.toAnimeUpdate
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.anime.model.toDomainAnime
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.animeextension.AnimeExtensionManager
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.toDomainAnime
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourcePresenter
import eu.kanade.tachiyomi.util.lang.runAsObservable
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import eu.kanade.domain.anime.model.Anime as DomainAnime

open class GlobalAnimeSearchPresenter(
    val initialQuery: String? = "",
    val initialExtensionFilter: String? = null,
    val sourceManager: AnimeSourceManager = Injekt.get(),
    val preferences: BasePreferences = Injekt.get(),
    val sourcePreferences: SourcePreferences = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
) : BasePresenter<GlobalAnimeSearchController>() {

    /**
     * Enabled sources.
     */
    val sources by lazy { getSourcesToQuery() }

    /**
     * Fetches the different sources by user settings.
     */
    private var fetchSourcesSubscription: Subscription? = null

    /**
     * Subject which fetches image of given anime.
     */
    private val fetchImageSubject = PublishSubject.create<Pair<List<DomainAnime>, AnimeSource>>()

    /**
     * Subscription for fetching images of anime.
     */
    private var fetchImageSubscription: Subscription? = null

    private val extensionManager: AnimeExtensionManager by injectLazy()

    private var extensionFilter: String? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        extensionFilter = savedState?.getString(GlobalAnimeSearchPresenter::extensionFilter.name)
            ?: initialExtensionFilter

        // Perform a search with previous or initial state
        search(
            savedState?.getString(BrowseAnimeSourcePresenter::query.name)
                ?: initialQuery.orEmpty(),
        )
    }

    override fun onDestroy() {
        fetchSourcesSubscription?.unsubscribe()
        fetchImageSubscription?.unsubscribe()
        super.onDestroy()
    }

    override fun onSave(state: Bundle) {
        state.putString(BrowseAnimeSourcePresenter::query.name, query)
        state.putString(GlobalAnimeSearchPresenter::extensionFilter.name, extensionFilter)
        super.onSave(state)
    }

    /**
     * Returns a list of enabled sources ordered by language and name, with pinned sources
     * prioritized.
     *
     * @return list containing enabled sources.
     */
    protected open fun getEnabledSources(): List<AnimeCatalogueSource> {
        val languages = sourcePreferences.enabledLanguages().get()
        val disabledSourceIds = sourcePreferences.disabledAnimeSources().get()
        val pinnedSourceIds = sourcePreferences.pinnedAnimeSources().get()

        return sourceManager.getCatalogueSources()
            .filter { it.lang in languages }
            .filterNot { it.id.toString() in disabledSourceIds }
            .sortedWith(compareBy({ it.id.toString() !in pinnedSourceIds }, { "${it.name.lowercase()} (${it.lang})" }))
    }

    private fun getSourcesToQuery(): List<AnimeCatalogueSource> {
        val filter = extensionFilter
        val enabledSources = getEnabledSources()
        var filteredSources: List<AnimeCatalogueSource>? = null

        if (!filter.isNullOrEmpty()) {
            filteredSources = extensionManager.installedExtensionsFlow.value
                .filter { it.pkgName == filter }
                .flatMap { it.sources }
                .filter { it in enabledSources }
                .filterIsInstance<AnimeCatalogueSource>()
        }

        if (filteredSources != null && filteredSources.isNotEmpty()) {
            return filteredSources
        }

        val onlyPinnedSources = sourcePreferences.searchPinnedSourcesOnly().get()
        val pinnedSourceIds = sourcePreferences.pinnedAnimeSources().get()

        return enabledSources
            .filter { if (onlyPinnedSources) it.id.toString() in pinnedSourceIds else true }
    }

    /**
     * Creates a catalogue search item
     */
    protected open fun createCatalogueSearchItem(source: AnimeCatalogueSource, results: List<GlobalAnimeSearchCardItem>?): GlobalAnimeSearchItem {
        return GlobalAnimeSearchItem(source, results)
    }

    /**
     * Initiates a search for anime per catalogue.
     *
     * @param query query on which to search.
     */
    fun search(query: String) {
        // Return if there's nothing to do
        if (this.query == query) return

        // Update query
        this.query = query

        // Create image fetch subscription
        initializeFetchImageSubscription()

        // Create items with the initial state
        val initialItems = sources.map { createCatalogueSearchItem(it, null) }
        var items = initialItems

        val pinnedSourceIds = sourcePreferences.pinnedAnimeSources().get()

        fetchSourcesSubscription?.unsubscribe()
        fetchSourcesSubscription = Observable.from(sources)
            .flatMap(
                { source ->
                    Observable.defer { source.fetchSearchAnime(1, query, source.getFilterList()) }
                        .subscribeOn(Schedulers.io())
                        .onErrorReturn { AnimesPage(emptyList(), false) } // Ignore timeouts or other exceptions
                        .map { it.animes }
                        .map { list -> list.map { runBlocking { networkToLocalAnime(it, source.id) } } } // Convert to local anime
                        .doOnNext { fetchImage(it, source) } // Load anime covers
                        .map { list -> createCatalogueSearchItem(source, list.map { GlobalAnimeSearchCardItem(it) }) }
                },
                5,
            )
            .observeOn(AndroidSchedulers.mainThread())
            // Update matching source with the obtained results
            .map { result ->
                items
                    .map { item -> if (item.source == result.source) result else item }
                    .sortedWith(
                        compareBy(
                            // Bubble up sources that actually have results
                            { it.results.isNullOrEmpty() },
                            // Same as initial sort, i.e. pinned first then alphabetically
                            { it.source.id.toString() !in pinnedSourceIds },
                            { "${it.source.name.lowercase()} (${it.source.lang})" },
                        ),
                    )
            }
            // Update current state
            .doOnNext { items = it }
            // Deliver initial state
            .startWith(initialItems)
            .subscribeLatestCache(
                { view, anime ->
                    view.setItems(anime)
                },
                { _, error ->
                    logcat(LogPriority.ERROR, error)
                },
            )
    }

    /**
     * Initialize a list of anime.
     *
     * @param anime the list of anime to initialize.
     */
    private fun fetchImage(anime: List<DomainAnime>, source: AnimeSource) {
        fetchImageSubject.onNext(Pair(anime, source))
    }

    /**
     * Subscribes to the initializer of anime details and updates the view if needed.
     */
    private fun initializeFetchImageSubscription() {
        fetchImageSubscription?.unsubscribe()
        fetchImageSubscription = fetchImageSubject.observeOn(Schedulers.io())
            .flatMap { (first, source) ->
                Observable.from(first)
                    .filter { it.thumbnailUrl == null && !it.initialized }
                    .map { Pair(it, source) }
                    .concatMap { runAsObservable { getAnimeDetails(it.first.toDbAnime(), it.second) } }
                    .map { Pair(source as AnimeCatalogueSource, it) }
            }
            .onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { (source, anime) ->
                    @Suppress("DEPRECATION")
                    view?.onAnimeInitialized(source, anime.toDomainAnime()!!)
                },
                { error ->
                    logcat(LogPriority.ERROR, error)
                },
            )
    }

    /**
     * Initializes the given anime.
     *
     * @param anime the anime to initialize.
     * @return The initialized anime.
     */
    private suspend fun getAnimeDetails(anime: Anime, source: AnimeSource): Anime {
        val networkAnime = source.getAnimeDetails(anime.copy())
        anime.copyFrom(networkAnime)
        anime.initialized = true
        updateAnime.await(anime.toDomainAnime()!!.toAnimeUpdate())
        return anime
    }

    /**
     * Returns an anime from the database for the given anime from network. It creates a new entry
     * if the anime is not yet in the database.
     *
     * @param sAnime the anime from the source.
     * @return an anime from the database.
     */
    protected open suspend fun networkToLocalAnime(sAnime: SAnime, sourceId: Long): DomainAnime {
        return networkToLocalAnime.await(sAnime.toDomainAnime(sourceId))
    }
}
