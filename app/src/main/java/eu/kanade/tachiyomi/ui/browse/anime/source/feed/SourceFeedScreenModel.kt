package eu.kanade.tachiyomi.ui.browse.anime.source.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.core.preference.asState
import eu.kanade.domain.entries.anime.model.toDomainAnime
import eu.kanade.domain.source.anime.interactor.GetExhSavedSearch
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.browse.anime.SourceFeedUI
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.ui.browse.feed.MAX_FEED_ITEMS
import exh.util.nullIfBlank
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.source.anime.interactor.CountFeedSavedSearchBySourceId
import tachiyomi.domain.source.anime.interactor.DeleteFeedSavedSearchById
import tachiyomi.domain.source.anime.interactor.GetFeedSavedSearchBySourceId
import tachiyomi.domain.source.anime.interactor.GetSavedSearchBySourceIdFeed
import tachiyomi.domain.source.anime.interactor.InsertFeedSavedSearch
import tachiyomi.domain.source.anime.interactor.ReorderFeed
import tachiyomi.domain.source.anime.model.EXHSavedSearch
import tachiyomi.domain.source.anime.model.FeedSavedSearch
import tachiyomi.domain.source.anime.model.FeedSavedSearchUpdate
import tachiyomi.domain.source.anime.model.SavedSearch
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.i18n.tail.TLMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import xyz.nulldev.ts.api.http.serializer.FilterSerializer
import java.util.concurrent.Executors
import tachiyomi.domain.entries.anime.model.Anime as DomainAnime

open class SourceFeedScreenModel(
    val sourceId: Long,
    uiPreferences: UiPreferences = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val getFeedSavedSearchBySourceId: GetFeedSavedSearchBySourceId = Injekt.get(),
    private val getSavedSearchBySourceIdFeed: GetSavedSearchBySourceIdFeed = Injekt.get(),
    private val countFeedSavedSearchBySourceId: CountFeedSavedSearchBySourceId = Injekt.get(),
    private val insertFeedSavedSearch: InsertFeedSavedSearch = Injekt.get(),
    private val deleteFeedSavedSearchById: DeleteFeedSavedSearchById = Injekt.get(),
    private val getExhSavedSearch: GetExhSavedSearch = Injekt.get(),
    // KMK -->
    private val reorderFeed: ReorderFeed = Injekt.get(),
    // KMK <--
) : StateScreenModel<SourceFeedState>(SourceFeedState()) {

    var source = sourceManager.getOrStub(sourceId)

    private val coroutineDispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()

    val startExpanded by uiPreferences.expandFilters().asState(screenModelScope)

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

            setFilters(source.getFilterList())
            // KMK -->
            reloadSavedSearches()
            // KMK <--
            getFeedSavedSearchBySourceId.subscribe(source.id)
                .onEach {
                    val items = getSourcesToGetFeed(it)
                    mutableState.update { state ->
                        state.copy(
                            items = items,
                        )
                    }
                    getFeed(items)
                }
                .launchIn(screenModelScope)
        }
    }

    // KMK-->
    fun resetFilters() {
        val source = source
        if (source !is AnimeCatalogueSource) return

        setFilters(source.getFilterList())

        reloadSavedSearches()
    }
    // KMK <--

    fun setFilters(filters: AnimeFilterList) {
        mutableState.update { it.copy(filters = filters) }
    }

    private suspend fun hasTooManyFeeds(): Boolean {
        return countFeedSavedSearchBySourceId.await(source.id) > MAX_FEED_ITEMS
    }

    fun createFeed(savedSearchId: Long) {
        screenModelScope.launchNonCancellable {
            insertFeedSavedSearch.await(
                FeedSavedSearch(
                    id = -1,
                    source = source.id,
                    savedSearch = savedSearchId,
                    global = false,
                    feedOrder = 0,
                ),
            )
        }
    }

    fun deleteFeed(feed: FeedSavedSearch) {
        screenModelScope.launchNonCancellable {
            deleteFeedSavedSearchById.await(feed.id)
        }
    }

    // KMK -->
    fun moveUp(feed: FeedSavedSearch) {
        screenModelScope.launch {
            reorderFeed.moveUp(feed, false)
        }
    }

    fun moveDown(feed: FeedSavedSearch) {
        screenModelScope.launch {
            reorderFeed.moveDown(feed, false)
        }
    }

    fun sortAlphabetically() {
        screenModelScope.launchNonCancellable {
            reorderFeed.sortAlphabetically(
                state.value.items
                    .filterIsInstance<SourceFeedUI.SourceSavedSearch>()
                    .sortedBy { feed -> feed.title }
                    .mapIndexed { index, feed ->
                        FeedSavedSearchUpdate(
                            id = feed.feed.id,
                            feedOrder = index.toLong(),
                        )
                    },
            )
        }
    }
    // KMK <--

    private suspend fun getSourcesToGetFeed(feedSavedSearch: List<FeedSavedSearch>): ImmutableList<SourceFeedUI> {
        // KMK -->
        val source = source
        // KMK <--
        if (source !is AnimeCatalogueSource) return persistentListOf()
        val savedSearches = getSavedSearchBySourceIdFeed.await(source.id)
            .associateBy { it.id }

        return (
            listOfNotNull(
                if (source.supportsLatest) {
                    SourceFeedUI.Latest(null)
                } else {
                    null
                },
                SourceFeedUI.Browse(null),
            ) + feedSavedSearch
                .map { SourceFeedUI.SourceSavedSearch(it, savedSearches[it.savedSearch]!!, null) }
            )
            .toImmutableList()
    }

    /**
     * Initiates get manga per feed.
     */
    private fun getFeed(feedSavedSearch: List<SourceFeedUI>) {
        // KMK -->
        val source = source
        // KMK <--
        if (source !is AnimeCatalogueSource) return
        screenModelScope.launch {
            feedSavedSearch.map { sourceFeed ->
                async {
                    val page = try {
                        withContext(coroutineDispatcher) {
                            when (sourceFeed) {
                                is SourceFeedUI.Browse -> source.getPopularAnime(1)
                                is SourceFeedUI.Latest -> source.getLatestUpdates(1)
                                is SourceFeedUI.SourceSavedSearch -> source.getSearchAnime(
                                    page = 1,
                                    query = sourceFeed.savedSearch.query.orEmpty(),
                                    filters = getFilterList(sourceFeed.savedSearch, source),
                                )
                            }
                        }.animes
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val titles = withIOContext {
                        page.map {
                            // KMK -->
                            it.toDomainAnime(source.id)
                            // KMK <--
                        }
                    }

                    mutableState.update { state ->
                        state.copy(
                            items = state.items.map { item ->
                                if (item.id == sourceFeed.id) sourceFeed.withResults(titles) else item
                            }.toImmutableList(),
                        )
                    }
                }
            }.awaitAll()
        }
    }

    private val filterSerializer = FilterSerializer()

    private fun getFilterList(savedSearch: SavedSearch, source: AnimeCatalogueSource): AnimeFilterList {
        val filters = savedSearch.filtersJson ?: return AnimeFilterList()
        return runCatching {
            val originalFilters = source.getFilterList()
            filterSerializer.deserialize(
                filters = originalFilters,
                json = Json.decodeFromString(filters),
            )
            originalFilters
        }.getOrElse { AnimeFilterList() }
    }

    @Composable
    fun getManga(initialManga: DomainAnime): State<DomainAnime> {
        return produceState(initialValue = initialManga) {
            getAnime.subscribe(initialManga.url, initialManga.source)
                .collectLatest { manga ->
                    value = manga
                        // KMK -->
                        ?: initialManga
                    // KMK <--
                }
        }
    }

    // KMK -->
    private fun reloadSavedSearches() {
        screenModelScope.launchIO {
            val searches = loadSearches()
            mutableState.update { it.copy(savedSearches = searches) }
        }
    }
    // KMK <--

    private suspend fun loadSearches() =
        getExhSavedSearch.await(source.id, (source as AnimeCatalogueSource)::getFilterList)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, EXHSavedSearch::name))
            .toImmutableList()

    fun onFilter(onBrowseClick: (query: String?, filters: String?) -> Unit) {
        // KMK -->
        val source = source
        // KMK <--
        if (source !is AnimeCatalogueSource) return
        screenModelScope.launchIO {
            val allDefault = state.value.filters == source.getFilterList()
            dismissDialog()
            if (allDefault) {
                onBrowseClick(
                    state.value.searchQuery?.nullIfBlank(),
                    null,
                )
            } else {
                onBrowseClick(
                    state.value.searchQuery?.nullIfBlank(),
                    Json.encodeToString(filterSerializer.serialize(state.value.filters)),
                )
            }
        }
    }

    /** Open a saved search */
    fun onSavedSearch(
        // KMK -->
        loadedSearch: EXHSavedSearch,
        // KMK <--
        onBrowseClick: (query: String?, searchId: Long) -> Unit,
        onToast: (StringResource) -> Unit,
    ) {
        // KMK -->
        val source = source
        // KMK <--
        if (source !is AnimeCatalogueSource) return
        screenModelScope.launchIO {
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
            dismissDialog()

            if (!allDefault) {
                onBrowseClick(
                    state.value.searchQuery?.nullIfBlank(),
                    search.id,
                )
            }
        }
    }

    fun onSavedSearchAddToFeed(
        search: EXHSavedSearch,
        onToast: (StringResource) -> Unit,
    ) {
        screenModelScope.launchIO {
            if (hasTooManyFeeds()) {
                withUIContext {
                    onToast(TLMR.strings.too_many_in_feed)
                }
                return@launchIO
            }
            openAddFeed(search.id, search.name)
        }
    }

    fun search(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun openFilterSheet() {
        mutableState.update { it.copy(dialog = Dialog.Filter) }
    }

    fun openDeleteFeed(feed: FeedSavedSearch) {
        mutableState.update { it.copy(dialog = Dialog.DeleteFeed(feed)) }
    }

    // KMK -->
    fun openActionsDialog(
        feed: SourceFeedUI.SourceSavedSearch,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
    ) {
        screenModelScope.launchIO {
            mutableState.update { state ->
                state.copy(
                    dialog = Dialog.FeedActions(
                        feedItem = feed,
                        canMoveUp = canMoveUp,
                        canMoveDown = canMoveDown,
                    ),
                )
            }
        }
    }

    fun showDialog(dialog: Dialog) {
        if (!state.value.isLoading) {
            mutableState.update {
                it.copy(dialog = dialog)
            }
        }
    }
    // KMK <--

    private fun openAddFeed(feedId: Long, name: String) {
        mutableState.update { it.copy(dialog = Dialog.AddFeed(feedId, name)) }
    }

    fun dismissDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    sealed class Dialog {
        data object Filter : Dialog()
        data class DeleteFeed(val feed: FeedSavedSearch) : Dialog()
        data class AddFeed(val feedId: Long, val name: String) : Dialog()

        // KMK -->
        data class FeedActions(
            val feedItem: SourceFeedUI.SourceSavedSearch,
            val canMoveUp: Boolean,
            val canMoveDown: Boolean,
        ) : Dialog()

        data object SortAlphabetically : Dialog()
        // KMK <--
    }

    override fun onDispose() {
        super.onDispose()
        coroutineDispatcher.close()
    }
}

@Immutable
data class SourceFeedState(
    val searchQuery: String? = null,
    val items: ImmutableList<SourceFeedUI> = persistentListOf(),
    val filters: AnimeFilterList = AnimeFilterList(),
    val savedSearches: ImmutableList<EXHSavedSearch> = persistentListOf(),
    val dialog: SourceFeedScreenModel.Dialog? = null,
) {
    val isLoading
        get() = items.isEmpty()
}
