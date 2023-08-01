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
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.preference.asState
import eu.kanade.domain.entries.anime.interactor.UpdateAnime
import eu.kanade.domain.entries.anime.model.toDomainAnime
import eu.kanade.domain.items.episode.interactor.SyncEpisodesWithTrackServiceTwoWay
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.anime.model.toDomainTrack
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.track.EnhancedAnimeTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.util.removeCovers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.preference.CheckboxState
import tachiyomi.core.preference.mapAsCheckboxState
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.anime.interactor.SetAnimeCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.interactor.GetDuplicateLibraryAnime
import tachiyomi.domain.entries.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.toAnimeUpdate
import tachiyomi.domain.items.episode.interactor.GetEpisodeByAnimeId
import tachiyomi.domain.items.episode.interactor.SetAnimeDefaultEpisodeFlags
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.anime.interactor.GetRemoteAnime
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.track.anime.interactor.InsertAnimeTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import eu.kanade.tachiyomi.animesource.model.AnimeFilter as AnimeSourceModelFilter

class BrowseAnimeSourceScreenModel(
    private val sourceId: Long,
    listingQuery: String?,
    sourceManager: AnimeSourceManager = Injekt.get(),
    sourcePreferences: SourcePreferences = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val coverCache: AnimeCoverCache = Injekt.get(),
    private val getRemoteAnime: GetRemoteAnime = Injekt.get(),
    private val getDuplicateAnimelibAnime: GetDuplicateLibraryAnime = Injekt.get(),
    private val getCategories: GetAnimeCategories = Injekt.get(),
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    private val setAnimeDefaultEpisodeFlags: SetAnimeDefaultEpisodeFlags = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val insertTrack: InsertAnimeTrack = Injekt.get(),
    private val syncEpisodesWithTrackServiceTwoWay: SyncEpisodesWithTrackServiceTwoWay = Injekt.get(),
) : StateScreenModel<BrowseAnimeSourceScreenModel.State>(State(Listing.valueOf(listingQuery))) {

    private val loggedServices by lazy { Injekt.get<TrackManager>().services.filter { it.isLogged } }

    var displayMode by sourcePreferences.sourceDisplayMode().asState(coroutineScope)

    val source = sourceManager.getOrStub(sourceId)

    init {
        if (source is AnimeCatalogueSource) {
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
        }
    }

    /**
     * Flow of Pager flow tied to [State.listing]
     */
    val animePagerFlowFlow = state.map { it.listing }
        .distinctUntilChanged()
        .map { listing ->
            Pager(
                PagingConfig(pageSize = 25),
            ) {
                getRemoteAnime.subscribe(sourceId, listing.query ?: "", listing.filters)
            }.flow.map { pagingData ->
                pagingData.map {
                    networkToLocalAnime.await(it.toDomainAnime(sourceId))
                        .let { localAnime ->
                            getAnime.subscribe(localAnime.url, localAnime.source)
                        }
                        .filterNotNull()
                        .filter { localAnime ->
                            !sourcePreferences.hideInAnimeLibraryItems().get() || !localAnime.favorite
                        }
                        .stateIn(ioCoroutineScope)
                }
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

    fun resetFilters() {
        if (source !is AnimeCatalogueSource) return

        mutableState.update { it.copy(filters = source.getFilterList()) }
    }

    fun setListing(listing: Listing) {
        mutableState.update { it.copy(listing = listing) }
    }

    fun setFilters(filters: AnimeFilterList) {
        if (source !is AnimeCatalogueSource) return

        mutableState.update {
            it.copy(
                filters = filters,
            )
        }
    }

    fun search(query: String? = null, filters: AnimeFilterList? = null) {
        if (source !is AnimeCatalogueSource) return

        val input = state.value.listing as? Listing.Search
            ?: Listing.Search(query = null, filters = source.getFilterList())

        mutableState.update {
            it.copy(
                listing = input.copy(
                    query = query ?: input.query,
                    filters = filters ?: input.filters,
                ),
                toolbarQuery = query ?: input.query,
            )
        }
    }

    fun searchGenre(genreName: String) {
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
        coroutineScope.launch {
            var new = anime.copy(
                favorite = !anime.favorite,
                dateAdded = when (anime.favorite) {
                    true -> 0
                    false -> Date().time
                },
            )

            if (!new.favorite) {
                new = new.removeCovers(coverCache)
            } else {
                setAnimeDefaultEpisodeFlags.await(anime)

                autoAddTrack(anime)
            }

            updateAnime.await(new.toAnimeUpdate())
        }
    }

    fun addFavorite(anime: Anime) {
        coroutineScope.launch {
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
                            categories.mapAsCheckboxState { it.id in preselectedIds },
                        ),
                    )
                }
            }
        }
    }

    private suspend fun autoAddTrack(anime: Anime) {
        loggedServices
            .filterIsInstance<EnhancedAnimeTrackService>()
            .filter { it.accept(source) }
            .forEach { service ->
                try {
                    service.match(anime)?.let { track ->
                        track.anime_id = anime.id
                        (service as TrackService).animeService.bind(track)
                        insertTrack.await(track.toDomainTrack()!!)

                        val chapters = getEpisodeByAnimeId.await(anime.id)
                        syncEpisodesWithTrackServiceTwoWay.await(chapters, track.toDomainTrack()!!, service.animeService)
                    }
                } catch (e: Exception) {
                    logcat(
                        LogPriority.WARN,
                        e,
                    ) { "Could not match anime: ${anime.title} with service $service" }
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
        return getDuplicateAnimelibAnime.await(anime.title)
    }

    private fun moveAnimeToCategories(anime: Anime, vararg categories: Category) {
        moveAnimeToCategories(anime, categories.filter { it.id != 0L }.map { it.id })
    }

    fun moveAnimeToCategories(anime: Anime, categoryIds: List<Long>) {
        coroutineScope.launchIO {
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

    sealed class Listing(open val query: String?, open val filters: AnimeFilterList) {
        object Popular : Listing(query = GetRemoteAnime.QUERY_POPULAR, filters = AnimeFilterList())
        object Latest : Listing(query = GetRemoteAnime.QUERY_LATEST, filters = AnimeFilterList())
        data class Search(override val query: String?, override val filters: AnimeFilterList) : Listing(query = query, filters = filters)

        companion object {
            fun valueOf(query: String?): Listing {
                return when (query) {
                    GetRemoteAnime.QUERY_POPULAR -> Popular
                    GetRemoteAnime.QUERY_LATEST -> Latest
                    else -> Search(query = query, filters = AnimeFilterList()) // filters are filled in later
                }
            }
        }
    }

    sealed class Dialog {
        object Filter : Dialog()
        data class RemoveAnime(val anime: Anime) : Dialog()
        data class AddDuplicateAnime(val anime: Anime, val duplicate: Anime) : Dialog()
        data class ChangeAnimeCategory(
            val anime: Anime,
            val initialSelection: List<CheckboxState.State<Category>>,
        ) : Dialog()
        data class Migrate(val newAnime: Anime) : Dialog()
    }

    @Immutable
    data class State(
        val listing: Listing,
        val filters: AnimeFilterList = AnimeFilterList(),
        val toolbarQuery: String? = null,
        val dialog: Dialog? = null,
    ) {
        val isUserQuery get() = listing is Listing.Search && !listing.query.isNullOrEmpty()
    }
}
