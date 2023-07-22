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
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.core.util.lang.awaitSingle
import tachiyomi.domain.entries.manga.interactor.GetManga
import tachiyomi.domain.entries.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.entries.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.Executors

abstract class MangaSearchScreenModel<T>(
    initialState: T,
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val extensionManager: MangaExtensionManager = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
) : StateScreenModel<T>(initialState) {

    private val coroutineDispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()

    protected var query: String? = null
    protected lateinit var extensionFilter: String

    private val sources by lazy { getSelectedSources() }
    private val pinnedSources by lazy { sourcePreferences.pinnedMangaSources().get() }

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
                .collectLatest { manga ->
                    if (manga == null) return@collectLatest
                    value = manga
                }
        }
    }

    abstract fun getEnabledSources(): List<CatalogueSource>

    private fun getSelectedSources(): List<CatalogueSource> {
        val filter = extensionFilter

        val enabledSources = getEnabledSources()

        if (filter.isEmpty()) {
            val shouldSearchPinnedOnly = sourcePreferences.searchPinnedMangaSourcesOnly().get()
            val pinnedSources = sourcePreferences.pinnedMangaSources().get()

            return enabledSources.filter {
                if (shouldSearchPinnedOnly) {
                    "${it.id}" in pinnedSources
                } else {
                    true
                }
            }
        }

        return extensionManager.installedExtensionsFlow.value
            .filter { it.pkgName == filter }
            .flatMap { it.sources }
            .filter { it in enabledSources }
            .filterIsInstance<CatalogueSource>()
    }

    abstract fun updateSearchQuery(query: String?)

    abstract fun updateItems(items: Map<CatalogueSource, MangaSearchItemResult>)

    abstract fun getItems(): Map<CatalogueSource, MangaSearchItemResult>

    private fun getAndUpdateItems(function: (Map<CatalogueSource, MangaSearchItemResult>) -> Map<CatalogueSource, MangaSearchItemResult>) {
        updateItems(function(getItems()))
    }

    fun search(query: String) {
        if (this.query == query) return

        this.query = query

        val initialItems = getSelectedSources().associateWith { MangaSearchItemResult.Loading }
        updateItems(initialItems)

        ioCoroutineScope.launch {
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
}
