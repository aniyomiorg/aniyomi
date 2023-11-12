package eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import cafe.adriel.voyager.core.model.StateScreenModel
import eu.kanade.domain.entries.anime.interactor.UpdateAnime
import eu.kanade.domain.entries.anime.model.toDomainAnime
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.core.util.lang.awaitSingle
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.Executors

abstract class AnimeSearchScreenModel<T>(
    initialState: T,
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val extensionManager: AnimeExtensionManager = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
) : StateScreenModel<T>(initialState) {

    private val coroutineDispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
    private var searchJob: Job? = null

    protected var query: String? = null
    protected var extensionFilter: String? = null

    private val sources by lazy { getSelectedSources() }
    protected val pinnedSources = sourcePreferences.pinnedAnimeSources().get()

    private val sortComparator = { map: Map<AnimeCatalogueSource, AnimeSearchItemResult> ->
        compareBy<AnimeCatalogueSource>(
            { (map[it] as? AnimeSearchItemResult.Success)?.isEmpty ?: true },
            { "${it.id}" !in pinnedSources },
            { "${it.name.lowercase()} (${it.lang})" },
        )
    }

    @Composable
    fun getAnime(initialAnime: Anime): State<Anime> {
        return produceState(initialValue = initialAnime) {
            getAnime.subscribe(initialAnime.url, initialAnime.source)
                .filterNotNull()
                .collectLatest { anime ->
                    value = anime
                }
        }
    }

    open fun getEnabledSources(): List<AnimeCatalogueSource> {
        val enabledLanguages = sourcePreferences.enabledLanguages().get()
        val disabledSources = sourcePreferences.disabledAnimeSources().get()
        val pinnedSources = sourcePreferences.pinnedAnimeSources().get()

        return sourceManager.getCatalogueSources()
            .filter { it.lang in enabledLanguages && "${it.id}" !in disabledSources }
            .sortedWith(
                compareBy(
                    { "${it.id}" !in pinnedSources },
                    { "${it.name.lowercase()} (${it.lang})" },
                ),
            )
    }

    private fun getSelectedSources(): List<AnimeCatalogueSource> {
        val enabledSources = getEnabledSources()

        val filter = extensionFilter
        if (filter.isNullOrEmpty()) {
            return enabledSources
        }

        return extensionManager.installedExtensionsFlow.value
            .filter { it.pkgName == filter }
            .flatMap { it.sources }
            .filterIsInstance<AnimeCatalogueSource>()
            .filter { it in enabledSources }
    }

    abstract fun updateSearchQuery(query: String?)

    abstract fun updateItems(items: Map<AnimeCatalogueSource, AnimeSearchItemResult>)

    abstract fun getItems(): Map<AnimeCatalogueSource, AnimeSearchItemResult>

    private fun getAndUpdateItems(function: (Map<AnimeCatalogueSource, AnimeSearchItemResult>) -> Map<AnimeCatalogueSource, AnimeSearchItemResult>) {
        updateItems(function(getItems()))
    }

    abstract fun setSourceFilter(filter: AnimeSourceFilter)

    abstract fun toggleFilterResults()

    fun search(query: String) {
        if (this.query == query) return

        this.query = query

        val initialItems = getSelectedSources().associateWith { AnimeSearchItemResult.Loading }
        updateItems(initialItems)
        searchJob = ioCoroutineScope.launch {
            sources
                .map { source ->
                    async {
                        try {
                            val page = withContext(coroutineDispatcher) {
                                source.fetchSearchAnime(1, query, source.getFilterList()).awaitSingle()
                            }

                            val titles = page.animes.map {
                                networkToLocalAnime.await(it.toDomainAnime(source.id))
                            }

                            getAndUpdateItems { items ->
                                val mutableMap = items.toMutableMap()
                                mutableMap[source] = AnimeSearchItemResult.Success(titles)
                                mutableMap.toSortedMap(sortComparator(mutableMap))
                            }
                        } catch (e: Exception) {
                            getAndUpdateItems { items ->
                                val mutableMap = items.toMutableMap()
                                mutableMap[source] = AnimeSearchItemResult.Error(e)
                                mutableMap.toSortedMap(sortComparator(mutableMap))
                            }
                        }
                    }
                }.awaitAll()
        }
    }
}

enum class AnimeSourceFilter {
    All,
    PinnedOnly,
}

sealed class AnimeSearchItemResult {
    object Loading : AnimeSearchItemResult()

    data class Error(
        val throwable: Throwable,
    ) : AnimeSearchItemResult()

    data class Success(
        val result: List<Anime>,
    ) : AnimeSearchItemResult() {
        val isEmpty: Boolean
            get() = result.isEmpty()
    }

    fun isVisible(onlyShowHasResults: Boolean): Boolean {
        return !onlyShowHasResults || (this is Success && !this.isEmpty)
    }
}
