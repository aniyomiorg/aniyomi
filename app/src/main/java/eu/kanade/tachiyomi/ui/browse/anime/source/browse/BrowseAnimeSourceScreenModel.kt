package eu.kanade.tachiyomi.ui.browse.anime.source.browse

import android.content.res.Configuration
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.core.preference.asState
import eu.kanade.domain.entries.anime.interactor.UpdateAnime
import eu.kanade.domain.entries.anime.model.toDomainAnime
import eu.kanade.domain.source.anime.interactor.GetAnimeIncognitoState
import eu.kanade.domain.source.anime.interactor.GetExhSavedSearch
import eu.kanade.domain.source.anime.interactor.InsertSavedSearch
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.anime.interactor.AddAnimeTracks
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.FilterList
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.util.removeCovers
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.anime.interactor.SetAnimeCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.interactor.GetDuplicateLibraryAnime
import tachiyomi.domain.entries.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.toAnimeUpdate
import tachiyomi.domain.items.episode.interactor.SetAnimeDefaultEpisodeFlags
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.anime.interactor.DeleteSavedSearchById
import tachiyomi.domain.source.anime.interactor.GetRemoteAnime
import tachiyomi.domain.source.anime.model.EXHSavedSearch
import tachiyomi.domain.source.anime.model.SavedSearch
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.i18n.tail.TLMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import xyz.nulldev.ts.api.http.serializer.FilterSerializer
import java.time.Instant
import eu.kanade.tachiyomi.animesource.model.AnimeFilter as AnimeSourceModelFilter

class BrowseAnimeSourceScreenModel(
    private val sourceId: Long,
    listingQuery: String?,
    // SY -->
    private val filtersJson: String? = null,
    private val savedSearch: Long? = null,
    // SY <--
    sourceManager: AnimeSourceManager = Injekt.get(),
    sourcePreferences: SourcePreferences = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val coverCache: AnimeCoverCache = Injekt.get(),
    private val getRemoteAnime: GetRemoteAnime = Injekt.get(),
    private val getDuplicateAnimelibAnime: GetDuplicateLibraryAnime = Injekt.get(),
    private val getCategories: GetAnimeCategories = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    private val setAnimeDefaultEpisodeFlags: SetAnimeDefaultEpisodeFlags = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val addTracks: AddAnimeTracks = Injekt.get(),
    private val getIncognitoState: GetAnimeIncognitoState = Injekt.get(),
    // SY -->
    uiPreferences: UiPreferences = Injekt.get(),
    private val deleteSavedSearchById: DeleteSavedSearchById = Injekt.get(),
    private val insertSavedSearch: InsertSavedSearch = Injekt.get(),
    private val getExhSavedSearch: GetExhSavedSearch = Injekt.get(),
    // SY <--
) : StateScreenModel<BrowseAnimeSourceScreenModel.State>(State(Listing.valueOf(listingQuery))) {

    var displayMode by sourcePreferences.sourceDisplayMode().asState(screenModelScope)

    var source = sourceManager.getOrStub(sourceId)

    // SY -->
    val startExpanded by uiPreferences.expandFilters().asState(screenModelScope)

    private val filterSerializer = FilterSerializer()

    // SY <--
    init {
        // KMK -->
        screenModelScope.launch {
            var retry = 10
            while (source !is AnimeCatalogueSource && retry-- > 0) {
                // Sometime source is late to load, so we need to wait a bit
                delay(100)
                source = sourceManager.getOrStub(sourceId)
            }
            val source = source
            if (source !is AnimeCatalogueSource) return@launch
            // KMK <--

            screenModelScope.launchIO {
                mutableState.update {
                    var query: String? = null
                    var listing = it.listing

                    if (listing is Listing.Search) {
                        query = listing.query
                        listing = Listing.Search(query, source.getFilterList())
                    }

                    it.copy(
                        listing = listing,
                        filters = source.getFilterList(),
                        toolbarQuery = query,
                    )
                }
            }.join()

            if (!getIncognitoState.await(source.id)) {
                sourcePreferences.lastUsedSource().set(source.id)
            }

            // SY -->
            val savedSearchId = savedSearch
            val jsonFilters = filtersJson
            val filters = state.value.filters
            if (savedSearchId != null) {
                val savedSearch = runBlocking { getExhSavedSearch.awaitOne(savedSearchId) { filters } }
                if (savedSearch != null) {
                    search(
                        query = savedSearch.query,
                        filters = savedSearch.filterList,
                        // KMK -->
                        savedSearchId = savedSearchId,
                        // KMK <--
                    )
                }
            } else if (jsonFilters != null) {
                runCatching {
                    val filtersJson = Json.decodeFromString<JsonArray>(jsonFilters)
                    filterSerializer.deserialize(filters, filtersJson)
                    search(filters = filters)
                }
            }

            getExhSavedSearch.subscribe(source.id, source::getFilterList)
                .map { it.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, EXHSavedSearch::name)) }
                .onEach { savedSearches ->
                    mutableState.update { it.copy(savedSearches = savedSearches.toImmutableList()) }
                }
                .launchIn(screenModelScope)
            // SY <--
        }
    }

    /**
     * Flow of Pager flow tied to [State.listing]
     */
    private val hideInLibraryItems = sourcePreferences.hideInAnimeLibraryItems().get()
    val animePagerFlowFlow = state.map { it.listing }
        .distinctUntilChanged()
        .map { listing ->
            Pager(PagingConfig(pageSize = 25)) {
                getRemoteAnime.subscribe(sourceId, listing.query ?: "", listing.filters)
            }.flow.map { pagingData ->
                pagingData.map {
                    networkToLocalAnime.await(it.toDomainAnime(sourceId))
                        .let { localAnime -> getAnime.subscribe(localAnime.url, localAnime.source) }
                        .filterNotNull()
                        .stateIn(ioCoroutineScope)
                }
                    .filter { !hideInLibraryItems || !it.value.favorite }
            }
                .cachedIn(ioCoroutineScope)
        }
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, emptyFlow())

    fun getColumnsPreference(orientation: Int): GridCells {
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        val columns = if (isLandscape) {
            libraryPreferences.animeLandscapeColumns()
        } else {
            libraryPreferences.animePortraitColumns()
        }.get()
        return if (columns == 0) GridCells.Adaptive(128.dp) else GridCells.Fixed(columns)
    }

    // returns the number from the size slider
    fun getColumnsPreferenceForCurrentOrientation(orientation: Int): Int {
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        return if (isLandscape) {
            libraryPreferences.animeLandscapeColumns()
        } else {
            libraryPreferences.animePortraitColumns()
        }.get()
    }

    fun resetFilters() {
        // KMK -->
        val source = source
        // KMK <--
        if (source !is AnimeCatalogueSource) return
        // KMK -->
        setFilters(source.getFilterList())

        reloadSavedSearches()
        // KMK <--

        mutableState.update { it.copy(filters = source.getFilterList()) }
    }

    fun setListing(listing: Listing) {
        mutableState.update { it.copy(listing = listing, toolbarQuery = null) }
    }

    fun setFilters(filters: FilterList) {
        if (source !is AnimeCatalogueSource) return

        mutableState.update {
            it.copy(
                filters = filters,
            )
        }
    }

    fun search(
        query: String? = null,
        filters: FilterList? = null,
        // KMK -->
        savedSearchId: Long? = null,
        // KMK <--
    ) {
        // KMK -->
        val source = source
        // KMK <--
        if (source !is AnimeCatalogueSource) return
        // SY -->
        if (filters != null && filters !== state.value.filters) {
            // KMK -->
            setFilters(filters)
            // KMK <--
        }
        // SY <--

        val input = state.value.listing as? Listing.Search
            ?: Listing.Search(query = null, filters = source.getFilterList())

        mutableState.update {
            it.copy(
                listing = input.copy(
                    query = query ?: input.query,
                    filters = filters ?: input.filters,
                    // KMK -->
                    savedSearchId = savedSearchId,
                    // KMK <--
                ),
                toolbarQuery = query ?: input.query,
            )
        }
    }

    fun searchGenre(genreName: String) {
        // KMK -->
        val source = source
        // KMK <--
        if (source !is AnimeCatalogueSource) return

        val defaultFilters = source.getFilterList()
        var genreExists = false

        filter@ for (sourceFilter in defaultFilters) {
            if (sourceFilter is AnimeSourceModelFilter.Group<*>) {
                for (filter in sourceFilter.state) {
                    if (filter is AnimeSourceModelFilter<*> && filter.name.equals(genreName, true)) {
                        when (filter) {
                            is AnimeSourceModelFilter.TriState -> filter.state = 1
                            is AnimeSourceModelFilter.CheckBox -> filter.state = true
                            else -> {}
                        }
                        genreExists = true
                        break@filter
                    }
                }
            } else if (sourceFilter is AnimeSourceModelFilter.Select<*>) {
                val index = sourceFilter.values.filterIsInstance<String>()
                    .indexOfFirst { it.equals(genreName, true) }

                if (index != -1) {
                    sourceFilter.state = index
                    genreExists = true
                    break
                }
            }
        }
        mutableState.update {
            val listing = if (genreExists) {
                Listing.Search(query = null, filters = defaultFilters)
            } else {
                Listing.Search(query = genreName, filters = defaultFilters)
            }
            it.copy(
                filters = defaultFilters,
                listing = listing,
                toolbarQuery = listing.query,
            )
        }
    }

    /**
     * Adds or removes an anime from the library.
     *
     * @param anime the anime to update.
     */
    fun changeAnimeFavorite(anime: Anime) {
        screenModelScope.launch {
            var new = anime.copy(
                favorite = !anime.favorite,
                dateAdded = when (anime.favorite) {
                    true -> 0
                    false -> Instant.now().toEpochMilli()
                },
            )

            if (!new.favorite) {
                new = new.removeCovers(coverCache)
            } else {
                setAnimeDefaultEpisodeFlags.await(anime)
                addTracks.bindEnhancedTrackers(anime, source)
            }

            updateAnime.await(new.toAnimeUpdate())
        }
    }

    fun addFavorite(anime: Anime) {
        screenModelScope.launch {
            val categories = getCategories()
            val defaultCategoryId = libraryPreferences.defaultAnimeCategory().get()
            val defaultCategory = categories.find { it.id == defaultCategoryId.toLong() }

            when {
                // Default category set
                defaultCategory != null -> {
                    moveAnimeToCategories(anime, defaultCategory)

                    changeAnimeFavorite(anime)
                }
                // Automatic 'Default' or no categories
                defaultCategoryId == 0 || categories.isEmpty() -> {
                    moveAnimeToCategories(anime)

                    changeAnimeFavorite(anime)
                }

                // Choose a category
                else -> {
                    val preselectedIds = getCategories.await(anime.id).map { it.id }
                    setDialog(
                        Dialog.ChangeAnimeCategory(
                            anime,
                            categories.mapAsCheckboxState { it.id in preselectedIds }.toImmutableList(),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return getCategories.subscribe()
            .firstOrNull()
            ?.filterNot { it.isSystemCategory }
            .orEmpty()
    }

    suspend fun getDuplicateAnimelibAnime(anime: Anime): Anime? {
        return getDuplicateAnimelibAnime.await(anime).getOrNull(0)
    }

    private fun moveAnimeToCategories(anime: Anime, vararg categories: Category) {
        moveAnimeToCategories(anime, categories.filter { it.id != 0L }.map { it.id })
    }

    fun moveAnimeToCategories(anime: Anime, categoryIds: List<Long>) {
        screenModelScope.launchIO {
            setAnimeCategories.await(
                animeId = anime.id,
                categoryIds = categoryIds.toList(),
            )
        }
    }

    fun openFilterSheet() {
        setDialog(Dialog.Filter)
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    fun setToolbarQuery(query: String?) {
        mutableState.update { it.copy(toolbarQuery = query) }
    }

    sealed class Listing(open val query: String?, open val filters: FilterList) {
        data object Popular : Listing(
            query = GetRemoteAnime.QUERY_POPULAR,
            filters = FilterList(),
        )
        data object Latest : Listing(
            query = GetRemoteAnime.QUERY_LATEST,
            filters = FilterList(),
        )
        data class Search(
            override val query: String?,
            override val filters: FilterList,
            // KMK -->
            val savedSearchId: Long? = null,
            // KMK <--
        ) : Listing(
            query = query,
            filters = filters,
        )

        companion object {
            fun valueOf(query: String?): Listing {
                return when (query) {
                    GetRemoteAnime.QUERY_POPULAR -> Popular
                    GetRemoteAnime.QUERY_LATEST -> Latest
                    else -> Search(query = query, filters = FilterList()) // filters are filled in later
                }
            }
        }
    }

    sealed interface Dialog {
        data object Filter : Dialog
        data class RemoveAnime(val anime: Anime) : Dialog
        data class AddDuplicateAnime(val anime: Anime, val duplicate: Anime) : Dialog
        data class ChangeAnimeCategory(
            val anime: Anime,
            val initialSelection: ImmutableList<CheckboxState.State<Category>>,
        ) : Dialog
        data class Migrate(val newAnime: Anime, val oldAnime: Anime) : Dialog

        // SY -->
        data class DeleteSavedSearch(val idToDelete: Long, val name: String) : Dialog
        data class CreateSavedSearch(val currentSavedSearches: ImmutableList<String>) : Dialog
        // SY <--
    }

    @Immutable
    data class State(
        val listing: Listing,
        val filters: FilterList = FilterList(),
        val toolbarQuery: String? = null,
        val dialog: Dialog? = null,
        // SY -->
        val savedSearches: ImmutableList<EXHSavedSearch> = persistentListOf(),
        val filterable: Boolean = true,
        // SY <--
    ) {
        val isUserQuery get() = listing is Listing.Search && !listing.query.isNullOrEmpty()
    }

    // KMK -->
    private fun reloadSavedSearches() {
        screenModelScope.launchIO {
            getExhSavedSearch.await(source.id, (source as AnimeCatalogueSource)::getFilterList)
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, EXHSavedSearch::name))
                .let { savedSearches ->
                    mutableState.update { it.copy(savedSearches = savedSearches.toImmutableList()) }
                }
        }
    }
    // KMK <--

    // EXH -->
    /** Show a dialog to enter name for new saved search */
    fun onSaveSearch() {
        screenModelScope.launchIO {
            val names = state.value.savedSearches.map { it.name }.toImmutableList()
            mutableState.update { it.copy(dialog = Dialog.CreateSavedSearch(names)) }
        }
    }

    /** Open a saved search */
    fun onSavedSearch(
        // KMK -->
        loadedSearch: EXHSavedSearch,
        // KMK <--
        onToast: (StringResource) -> Unit,
    ) {
        // KMK -->
        resetFilters()
        // KMK <--
        screenModelScope.launchIO {
            // KMK -->
            val source = source
            // KMK <--
            if (source !is AnimeCatalogueSource) return@launchIO

            // KMK -->
            val search = getExhSavedSearch.awaitOne(loadedSearch.id, source::getFilterList) ?: loadedSearch
            // KMK <--

            if (search.filterList == null && state.value.filters.isNotEmpty()) {
                withUIContext {
                    onToast(TLMR.strings.save_search_invalid)
                }
                return@launchIO
            }

            val allDefault = search.filterList != null && search.filterList == source.getFilterList()
            setDialog(null)

            val filters = search.filterList
                ?.takeUnless { allDefault }
                ?: source.getFilterList()

            mutableState.update {
                it.copy(
                    listing = Listing.Search(
                        query = search.query,
                        filters = filters,
                        // KMK -->
                        savedSearchId = search.id,
                        // KMK <--
                    ),
                    filters = filters,
                    toolbarQuery = search.query,
                )
            }
        }
    }

    /** Show dialog to delete saved search */
    fun onSavedSearchPress(search: EXHSavedSearch) {
        mutableState.update { it.copy(dialog = Dialog.DeleteSavedSearch(search.id, search.name)) }
    }

    /** Save a search */
    fun saveSearch(
        name: String,
    ) {
        // KMK -->
        val source = source
        // KMK <--
        if (source !is AnimeCatalogueSource) return
        screenModelScope.launchNonCancellable {
            val query = state.value.toolbarQuery?.takeUnless {
                it.isBlank() || it == GetRemoteAnime.QUERY_POPULAR || it == GetRemoteAnime.QUERY_LATEST
            }?.trim()
            val filterList = state.value.filters.ifEmpty { source.getFilterList() }
            insertSavedSearch.await(
                SavedSearch(
                    id = -1,
                    source = source.id,
                    name = name.trim(),
                    query = query,
                    filtersJson = runCatching {
                        filterSerializer.serialize(filterList).ifEmpty { null }?.let { Json.encodeToString(it) }
                    }.getOrNull(),
                ),
            )
        }
    }

    fun deleteSearch(savedSearchId: Long) {
        screenModelScope.launchNonCancellable {
            deleteSavedSearchById.await(savedSearchId)
        }
    }
    // EXH <--
}
