package eu.kanade.tachiyomi.ui.browse.feed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.util.fastAny
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.entries.anime.model.toDomainAnime
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.FeedItemUI
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.source.anime.interactor.CountFeedSavedSearchGlobal
import tachiyomi.domain.source.anime.interactor.DeleteFeedSavedSearchById
import tachiyomi.domain.source.anime.interactor.GetFeedSavedSearchGlobal
import tachiyomi.domain.source.anime.interactor.GetSavedSearchBySourceId
import tachiyomi.domain.source.anime.interactor.GetSavedSearchGlobalFeed
import tachiyomi.domain.source.anime.interactor.InsertFeedSavedSearch
import tachiyomi.domain.source.anime.interactor.ReorderFeed
import tachiyomi.domain.source.anime.model.FeedSavedSearch
import tachiyomi.domain.source.anime.model.FeedSavedSearchUpdate
import tachiyomi.domain.source.anime.model.SavedSearch
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import xyz.nulldev.ts.api.http.serializer.FilterSerializer
import java.util.concurrent.Executors
import tachiyomi.domain.entries.anime.model.Anime as DomainAnime

/**
 * Presenter of [feedTab]
 */
open class FeedScreenModel(
    val sourceManager: AnimeSourceManager = Injekt.get(),
    val sourcePreferences: SourcePreferences = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    getFeedSavedSearchGlobal: GetFeedSavedSearchGlobal = Injekt.get(),
    private val getSavedSearchGlobalFeed: GetSavedSearchGlobalFeed = Injekt.get(),
    private val countFeedSavedSearchGlobal: CountFeedSavedSearchGlobal = Injekt.get(),
    private val getSavedSearchBySourceId: GetSavedSearchBySourceId = Injekt.get(),
    private val insertFeedSavedSearch: InsertFeedSavedSearch = Injekt.get(),
    private val deleteFeedSavedSearchById: DeleteFeedSavedSearchById = Injekt.get(),
    // KMK -->
    private val reorderFeed: ReorderFeed = Injekt.get(),
    // KMK <--
) : StateScreenModel<FeedScreenState>(FeedScreenState()) {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    private val coroutineDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    var pushed: Boolean = false

    init {
        getFeedSavedSearchGlobal.subscribe()
            .distinctUntilChanged()
            .onEach {
                val items = getSourcesToGetFeed(it).map { (feed, savedSearch) ->
                    createCatalogueSearchItem(
                        feed = feed,
                        savedSearch = savedSearch,
                        source = sourceManager.get(feed.source) as? AnimeCatalogueSource,
                        results = null,
                    )
                }
                mutableState.update { state ->
                    state.copy(
                        items = items,
                    )
                }
                getFeed(items)
            }
            .catch { _events.send(Event.FailedFetchingSources) }
            .launchIn(screenModelScope)
    }

    fun init() {
        pushed = false
        screenModelScope.launchIO {
            val newItems = state.value.items?.map { it.copy(results = null) } ?: return@launchIO
            mutableState.update { state ->
                state.copy(
                    items = newItems,
                )
            }
            getFeed(newItems)
        }
    }

    fun openAddDialog() {
        screenModelScope.launchIO {
            if (hasTooManyFeeds()) {
                _events.send(Event.TooManyFeeds)
                return@launchIO
            }
            mutableState.update { state ->
                state.copy(
                    dialog = Dialog.AddFeed(getEnabledSources()),
                )
            }
        }
    }

    fun openAddSearchDialog(source: AnimeCatalogueSource) {
        screenModelScope.launchIO {
            mutableState.update { state ->
                state.copy(
                    dialog = Dialog.AddFeedSearch(
                        source,
                        (
                            // KMK -->
                            // (if (source.supportsLatest) persistentListOf(null) else persistentListOf()) +
                            persistentListOf(null) +
                                // KMK <-->
                                getSourceSavedSearches(source.id)
                            ).toImmutableList(),
                    ),
                )
            }
        }
    }

    fun openDeleteDialog(feed: FeedSavedSearch) {
        screenModelScope.launchIO {
            mutableState.update { state ->
                state.copy(
                    dialog = Dialog.DeleteFeed(feed),
                )
            }
        }
    }

    // KMK -->
    fun openActionsDialog(
        feed: FeedItemUI,
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
    // KMK <--

    private suspend fun hasTooManyFeeds(): Boolean {
        return countFeedSavedSearchGlobal.await() > MAX_FEED_ITEMS
    }

    private fun getEnabledSources(): ImmutableList<AnimeCatalogueSource> {
        val languages = sourcePreferences.enabledLanguages().get()
        val pinnedSources = sourcePreferences.pinnedSources().get()
        val disabledSources = sourcePreferences.disabledSources().get()
            .mapNotNull { it.toLongOrNull() }

        val list = sourceManager.getVisibleCatalogueSources()
            .filter { it.lang in languages }
            .filterNot { it.id in disabledSources }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { "(${it.lang}) ${it.name}" })

        return list.sortedBy { it.id.toString() !in pinnedSources }.toImmutableList()
    }

    private suspend fun getSourceSavedSearches(sourceId: Long): ImmutableList<SavedSearch> {
        return getSavedSearchBySourceId.await(sourceId).toImmutableList()
    }

    fun createFeed(source: AnimeCatalogueSource, savedSearch: SavedSearch?) {
        screenModelScope.launchNonCancellable {
            insertFeedSavedSearch.await(
                FeedSavedSearch(
                    id = -1,
                    source = source.id,
                    savedSearch = savedSearch?.id,
                    global = true,
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
            reorderFeed.moveUp(feed)
        }
    }

    fun moveDown(feed: FeedSavedSearch) {
        screenModelScope.launch {
            reorderFeed.moveDown(feed)
        }
    }

    fun sortAlphabetically() {
        screenModelScope.launch {
            reorderFeed.sortAlphabetically(
                state.value.items
                    ?.sortedBy { feed -> feed.title }
                    ?.mapIndexed { index, feed ->
                        FeedSavedSearchUpdate(
                            id = feed.feed.id,
                            feedOrder = index.toLong(),
                        )
                    },
            )
        }
    }
    // KMK <--

    private suspend fun getSourcesToGetFeed(
        feedSavedSearch: List<FeedSavedSearch>,
    ): List<
        Pair<
            FeedSavedSearch,
            SavedSearch?,
            >,
        > {
        val savedSearches = getSavedSearchGlobalFeed.await()
            .associateBy { it.id }
        return feedSavedSearch
            .map { it to savedSearches[it.savedSearch] }
    }

    /**
     * Creates a catalogue search item
     */
    private fun createCatalogueSearchItem(
        feed: FeedSavedSearch,
        savedSearch: SavedSearch?,
        source: AnimeCatalogueSource?,
        @Suppress("SameParameterValue") results: List<DomainAnime>?,
    ): FeedItemUI {
        return FeedItemUI(
            feed,
            savedSearch,
            source,
            savedSearch?.name ?: (source?.name ?: feed.source.toString()),
            if (savedSearch != null) {
                source?.name ?: feed.source.toString()
            } else {
                LocaleHelper.getLocalizedDisplayName(source?.lang)
            },
            results,
        )
    }

    // KMK -->
    private val hideInLibraryFeedItems = sourcePreferences.hideInLibraryFeedItems()
    // KMK <--

    /**
     * Initiates get manga per feed.
     */
    private fun getFeed(feedSavedSearch: List<FeedItemUI>) {
        screenModelScope.launch {
            feedSavedSearch.map { itemUI ->
                async {
                    val page = try {
                        if (itemUI.source != null) {
                            withContext(coroutineDispatcher) {
                                if (itemUI.savedSearch == null) {
                                    // KMK -->
                                    if (itemUI.source.supportsLatest) {
                                        // KMK <--
                                        itemUI.source.getLatestUpdates(1)
                                        // KMK -->
                                    } else {
                                        itemUI.source.getPopularAnime(1)
                                    }
                                    // KMK <--
                                } else {
                                    itemUI.source.getSearchAnime(
                                        1,
                                        itemUI.savedSearch.query.orEmpty(),
                                        getFilterList(itemUI.savedSearch, itemUI.source),
                                    )
                                }
                            }.animes
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val result = withIOContext {
                        itemUI.copy(
                            results = page.map {
                                // KMK -->
                                it.toDomainAnime(itemUI.source!!.id)
                            }
                                .filter { !hideInLibraryFeedItems.get() || !it.favorite },
                            // KMK <--
                        )
                    }

                    mutableState.update { state ->
                        state.copy(
                            items = state.items?.map { if (it.feed.id == result.feed.id) result else it },
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
    fun getManga(initialanime: DomainAnime): State<DomainAnime> {
        return produceState(initialValue = initialanime) {
            getAnime.subscribe(initialanime.url, initialanime.source)
                .collectLatest { anime ->
                    value = anime
                        // KMK -->
                        ?: initialanime
                    // KMK <--
                }
        }
    }
    override fun onDispose() {
        super.onDispose()
        coroutineDispatcher.close()
    }

    // KMK -->
    fun showDialog(dialog: Dialog) {
        if (!state.value.isLoading) {
            mutableState.update {
                it.copy(dialog = dialog)
            }
        }
    }
    // KMK <--

    fun dismissDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    sealed class Dialog {
        data class AddFeed(val options: ImmutableList<AnimeCatalogueSource>) : Dialog()
        data class AddFeedSearch(val source: AnimeCatalogueSource, val options: ImmutableList<SavedSearch?>) : Dialog()
        data class DeleteFeed(val feed: FeedSavedSearch) : Dialog()

        // KMK -->
        data class FeedActions(
            val feedItem: FeedItemUI,
            val canMoveUp: Boolean,
            val canMoveDown: Boolean,
        ) : Dialog()

        data object SortAlphabetically : Dialog()
        // KMK <--
    }

    sealed class Event {
        data object FailedFetchingSources : Event()
        data object TooManyFeeds : Event()
    }
}

data class FeedScreenState(
    val dialog: FeedScreenModel.Dialog? = null,
    val items: List<FeedItemUI>? = null,
) {
    val isLoading
        get() = items == null

    val isEmpty
        get() = items.isNullOrEmpty()

    val isLoadingItems
        get() = items?.fastAny { it.results == null } != false
}

const val MAX_FEED_ITEMS = 20
