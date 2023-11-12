package eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import cafe.adriel.voyager.core.model.StateScreenModel
import eu.kanade.domain.entries.manga.interactor.UpdateManga
import eu.kanade.domain.entries.manga.model.toDomainManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.core.util.lang.awaitSingle
import tachiyomi.domain.entries.manga.interactor.GetManga
import tachiyomi.domain.entries.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.source.manga.service.MangaSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.Executors

abstract class MangaSearchScreenModel<T>(
    initialState: T,
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: MangaSourceManager = Injekt.get(),
    private val extensionManager: MangaExtensionManager = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
) : StateScreenModel<T>(initialState) {

    private val coroutineDispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
    private var searchJob: Job? = null

    protected var query: String? = null
    protected var extensionFilter: String? = null

    private val sources by lazy { getSelectedSources() }
    protected val pinnedSources = sourcePreferences.pinnedMangaSources().get()

    private val sortComparator = { map: Map<CatalogueSource, MangaSearchItemResult> ->
        compareBy<CatalogueSource>(
            { (map[it] as? MangaSearchItemResult.Success)?.isEmpty ?: true },
            { "${it.id}" !in pinnedSources },
            { "${it.name.lowercase()} (${it.lang})" },
        )
    }

    @Composable
    fun getManga(initialManga: Manga): State<Manga> {
        return produceState(initialValue = initialManga) {
            getManga.subscribe(initialManga.url, initialManga.source)
                .filterNotNull()
                .collectLatest { manga ->
                    value = manga
                }
        }
    }

    open fun getEnabledSources(): List<CatalogueSource> {
        val enabledLanguages = sourcePreferences.enabledLanguages().get()
        val disabledSources = sourcePreferences.disabledMangaSources().get()
        val pinnedSources = sourcePreferences.pinnedMangaSources().get()

        return sourceManager.getCatalogueSources()
            .filter { it.lang in enabledLanguages && "${it.id}" !in disabledSources }
            .sortedWith(
                compareBy(
                    { "${it.id}" !in pinnedSources },
                    { "${it.name.lowercase()} (${it.lang})" },
                ),
            )
    }

    private fun getSelectedSources(): List<CatalogueSource> {
        val enabledSources = getEnabledSources()

        val filter = extensionFilter
        if (filter.isNullOrEmpty()) {
            return enabledSources
        }

        return extensionManager.installedExtensionsFlow.value
            .filter { it.pkgName == filter }
            .flatMap { it.sources }
            .filterIsInstance<CatalogueSource>()
            .filter { it in enabledSources }
    }

    abstract fun updateSearchQuery(query: String?)

    abstract fun updateItems(items: Map<CatalogueSource, MangaSearchItemResult>)

    abstract fun getItems(): Map<CatalogueSource, MangaSearchItemResult>

    private fun getAndUpdateItems(function: (Map<CatalogueSource, MangaSearchItemResult>) -> Map<CatalogueSource, MangaSearchItemResult>) {
        updateItems(function(getItems()))
    }

    abstract fun setSourceFilter(filter: MangaSourceFilter)

    abstract fun toggleFilterResults()

    fun search(query: String) {
        if (this.query == query) return

        this.query = query

        searchJob?.cancel()
        val initialItems = getSelectedSources().associateWith { MangaSearchItemResult.Loading }
        updateItems(initialItems)
        searchJob = ioCoroutineScope.launch {
            sources
                .map { source ->
                    async {
                        try {
                            val page = withContext(coroutineDispatcher) {
                                source.fetchSearchManga(1, query, source.getFilterList()).awaitSingle()
                            }

                            val titles = page.mangas.map {
                                networkToLocalManga.await(it.toDomainManga(source.id))
                            }

                            getAndUpdateItems { items ->
                                val mutableMap = items.toMutableMap()
                                mutableMap[source] = MangaSearchItemResult.Success(titles)
                                mutableMap.toSortedMap(sortComparator(mutableMap))
                            }
                        } catch (e: Exception) {
                            getAndUpdateItems { items ->
                                val mutableMap = items.toMutableMap()
                                mutableMap[source] = MangaSearchItemResult.Error(e)
                                mutableMap.toSortedMap(sortComparator(mutableMap))
                            }
                        }
                    }
                }.awaitAll()
        }
    }
}

enum class MangaSourceFilter {
    All,
    PinnedOnly,
}

sealed class MangaSearchItemResult {
    object Loading : MangaSearchItemResult()

    data class Error(
        val throwable: Throwable,
    ) : MangaSearchItemResult()

    data class Success(
        val result: List<Manga>,
    ) : MangaSearchItemResult() {
        val isEmpty: Boolean
            get() = result.isEmpty()
    }

    fun isVisible(onlyShowHasResults: Boolean): Boolean {
        return !onlyShowHasResults || (this is Success && !this.isEmpty)
    }
}
