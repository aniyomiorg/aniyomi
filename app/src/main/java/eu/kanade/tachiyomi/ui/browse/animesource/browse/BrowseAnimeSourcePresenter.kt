package eu.kanade.tachiyomi.ui.browse.animesource.browse

import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.core.prefs.CheckboxState
import eu.kanade.core.prefs.mapAsCheckboxState
import eu.kanade.domain.anime.interactor.GetAnime
import eu.kanade.domain.anime.interactor.GetDuplicateLibraryAnime
import eu.kanade.domain.anime.interactor.NetworkToLocalAnime
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.toAnimeUpdate
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.anime.model.toDomainAnime
import eu.kanade.domain.animesource.interactor.GetRemoteAnime
import eu.kanade.domain.animetrack.interactor.InsertAnimeTrack
import eu.kanade.domain.animetrack.model.toDomainTrack
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.category.interactor.GetAnimeCategories
import eu.kanade.domain.category.interactor.SetAnimeCategories
import eu.kanade.domain.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.episode.interactor.SetAnimeDefaultEpisodeFlags
import eu.kanade.domain.episode.interactor.SyncEpisodesWithTrackServiceTwoWay
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.animebrowse.BrowseAnimeSourceState
import eu.kanade.presentation.animebrowse.BrowseAnimeSourceStateImpl
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
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
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import eu.kanade.domain.anime.model.Anime as DomainAnime
import eu.kanade.domain.category.model.Category as DomainCategory

open class BrowseAnimeSourcePresenter(
    private val sourceId: Long,
    searchQuery: String? = null,
    private val state: BrowseAnimeSourceStateImpl = BrowseAnimeSourceState(searchQuery) as BrowseAnimeSourceStateImpl,
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    preferences: BasePreferences = Injekt.get(),
    sourcePreferences: SourcePreferences = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val coverCache: AnimeCoverCache = Injekt.get(),
    private val getRemoteAnime: GetRemoteAnime = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val getDuplicateLibraryAnime: GetDuplicateLibraryAnime = Injekt.get(),
    private val getCategories: GetAnimeCategories = Injekt.get(),
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    private val setAnimeDefaultEpisodeFlags: SetAnimeDefaultEpisodeFlags = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val insertTrack: InsertAnimeTrack = Injekt.get(),
    private val syncEpisodesWithTrackServiceTwoWay: SyncEpisodesWithTrackServiceTwoWay = Injekt.get(),
) : BasePresenter<BrowseAnimeSourceController>(), BrowseAnimeSourceState by state {

    private val loggedServices by lazy { Injekt.get<TrackManager>().services.filter { it.isLogged } }

    var displayMode by sourcePreferences.sourceDisplayMode().asState()

    val isDownloadOnly: Boolean by preferences.downloadedOnly().asState()
    val isIncognitoMode: Boolean by preferences.incognitoMode().asState()

    @Composable
    fun getColumnsPreferenceForCurrentOrientation(): State<GridCells> {
        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        return produceState<GridCells>(initialValue = GridCells.Adaptive(128.dp), isLandscape) {
            (if (isLandscape) libraryPreferences.landscapeColumns() else libraryPreferences.portraitColumns())
                .changes()
                .collectLatest { columns ->
                    value =
                        if (columns == 0) GridCells.Adaptive(128.dp) else GridCells.Fixed(columns)
                }
        }
    }

    @Composable
    fun getAnimeList(): Flow<PagingData<DomainAnime>> {
        return remember(currentFilter) {
            Pager(
                PagingConfig(pageSize = 25),
            ) {
                getRemoteAnime.subscribe(sourceId, currentFilter.query, currentFilter.filters)
            }.flow
                .map {
                    it.map { sAnime ->
                        withIOContext {
                            networkToLocalAnime.await(sAnime.toDomainAnime(sourceId))
                        }
                    }
                }
                .cachedIn(presenterScope)
        }
    }

    @Composable
    fun getAnime(initialAnime: DomainAnime): State<DomainAnime> {
        return produceState(initialValue = initialAnime) {
            getAnime.subscribe(initialAnime.url, initialAnime.source)
                .collectLatest { anime ->
                    if (anime == null) return@collectLatest
                    withIOContext {
                        initializeAnime(anime)
                    }
                    value = anime
                }
        }
    }

    fun reset() {
        state.filters = source!!.getFilterList()
        if (currentFilter !is AnimeFilter.UserInput) return
        state.currentFilter = (currentFilter as AnimeFilter.UserInput).copy(filters = state.filters)
    }

    fun search(query: String? = null, filters: AnimeFilterList? = null) {
        AnimeFilter.valueOf(query ?: "").let {
            if (it !is AnimeFilter.UserInput) {
                state.currentFilter = it
                return
            }
        }
        val input: AnimeFilter.UserInput =
            if (currentFilter is AnimeFilter.UserInput) currentFilter as AnimeFilter.UserInput else AnimeFilter.UserInput()
        state.currentFilter = input.copy(
            query = query ?: input.query,
            filters = filters ?: input.filters,
        )
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        state.source = sourceManager.get(sourceId) as? AnimeCatalogueSource ?: return
        state.filters = source!!.getFilterList()
    }

    /**
     * Initialize an anime.
     *
     * @return anime to initialize.
     */
    private suspend fun initializeAnime(anime: DomainAnime) {
        if (anime.thumbnailUrl != null || anime.initialized) return
        withNonCancellableContext {
            try {
                val networkAnime = source!!.getAnimeDetails(anime.toSAnime())
                val updatedAnime = anime.copyFrom(networkAnime)
                    .copy(initialized = true)

                updateAnime.await(updatedAnime.toAnimeUpdate())
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
            }
        }
    }

    /**
     * Adds or removes an anime from the library.
     *
     * @param anime the anime to update.
     */
    fun changeAnimeFavorite(anime: DomainAnime) {
        presenterScope.launch {
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

    fun getSourceOrStub(anime: DomainAnime): AnimeSource {
        return sourceManager.getOrStub(anime.source)
    }

    fun addFavorite(anime: DomainAnime) {
        presenterScope.launch {
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
                    state.dialog = Dialog.ChangeAnimeCategory(
                        anime,
                        categories.mapAsCheckboxState { it.id in preselectedIds },
                    )
                }
            }
        }
    }

    private suspend fun autoAddTrack(anime: DomainAnime) {
        loggedServices
            .filterIsInstance<EnhancedTrackService>()
            .filter { it.accept(source!!) }
            .forEach { service ->
                try {
                    service.match(anime.toDbAnime())?.let { track ->
                        track.anime_id = anime.id
                        (service as TrackService).bind(track)
                        insertTrack.await(track.toDomainTrack()!!)

                        val chapters = getEpisodeByAnimeId.await(anime.id)
                        syncEpisodesWithTrackServiceTwoWay.await(
                            chapters,
                            track.toDomainTrack()!!,
                            service,
                        )
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
    suspend fun getCategories(): List<DomainCategory> {
        return getCategories.subscribe()
            .firstOrNull()
            ?.filterNot { it.isSystemCategory }
            ?: emptyList()
    }

    suspend fun getDuplicateLibraryAnime(anime: DomainAnime): DomainAnime? {
        return getDuplicateLibraryAnime.await(anime.title, anime.source)
    }

    fun moveAnimeToCategories(anime: DomainAnime, vararg categories: DomainCategory) {
        moveAnimeToCategories(anime, categories.filter { it.id != 0L }.map { it.id })
    }

    fun moveAnimeToCategories(anime: DomainAnime, categoryIds: List<Long>) {
        presenterScope.launchIO {
            setAnimeCategories.await(
                animeId = anime.id,
                categoryIds = categoryIds.toList(),
            )
        }
    }

    sealed class AnimeFilter(open val query: String, open val filters: AnimeFilterList) {
        object Popular : AnimeFilter(query = GetRemoteAnime.QUERY_POPULAR, filters = AnimeFilterList())
        object Latest : AnimeFilter(query = GetRemoteAnime.QUERY_LATEST, filters = AnimeFilterList())
        data class UserInput(
            override val query: String = "",
            override val filters: AnimeFilterList = AnimeFilterList(),
        ) : AnimeFilter(query = query, filters = filters)

        companion object {
            fun valueOf(query: String): AnimeFilter {
                return when (query) {
                    GetRemoteAnime.QUERY_POPULAR -> Popular
                    GetRemoteAnime.QUERY_LATEST -> Latest
                    else -> UserInput(query = query)
                }
            }
        }
    }

    sealed class Dialog {
        data class RemoveAnime(val anime: DomainAnime) : Dialog()
        data class AddDuplicateAnime(val anime: DomainAnime, val duplicate: DomainAnime) : Dialog()
        data class ChangeAnimeCategory(
            val anime: DomainAnime,
            val initialSelection: List<CheckboxState.State<DomainCategory>>,
        ) : Dialog()
    }
}

fun AnimeFilterList.toItems(): List<IFlexible<*>> {
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
