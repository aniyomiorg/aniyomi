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
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.core.util.lang.awaitSingle
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.entries.anime.model.Anime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.Executors

abstract class AnimeSearchScreenModel<T>(
    initialState: T,
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val extensionManager: AnimeExtensionManager = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
) : StateScreenModel<T>(initialState) {

    private val coroutineDispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()

    protected var query: String? = null
    protected lateinit var extensionFilter: String

    private val sources by lazy { getSelectedSources() }
    private val pinnedSources by lazy { sourcePreferences.pinnedAnimeSources().get() }

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
                .collectLatest { anime ->
                    if (anime == null) return@collectLatest
                    value = anime
                }
        }
    }

    abstract fun getEnabledSources(): List<AnimeCatalogueSource>

    private fun getSelectedSources(): List<AnimeCatalogueSource> {
        val filter = extensionFilter

        val enabledSources = getEnabledSources()

        if (filter.isEmpty()) {
            val shouldSearchPinnedOnly = sourcePreferences.searchPinnedAnimeSourcesOnly().get()
            val pinnedSources = sourcePreferences.pinnedAnimeSources().get()

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
            .filterIsInstance<AnimeCatalogueSource>()
    }

    abstract fun updateSearchQuery(query: String?)

    abstract fun updateItems(items: Map<AnimeCatalogueSource, AnimeSearchItemResult>)

    abstract fun getItems(): Map<AnimeCatalogueSource, AnimeSearchItemResult>

    private fun getAndUpdateItems(function: (Map<AnimeCatalogueSource, AnimeSearchItemResult>) -> Map<AnimeCatalogueSource, AnimeSearchItemResult>) {
        updateItems(function(getItems()))
    }

    fun search(query: String) {
        if (this.query == query) return

        this.query = query

        val initialItems = getSelectedSources().associateWith { AnimeSearchItemResult.Loading }
        updateItems(initialItems)

        ioCoroutineScope.launch {
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
}
