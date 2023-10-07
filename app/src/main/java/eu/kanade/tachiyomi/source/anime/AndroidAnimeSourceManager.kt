package eu.kanade.tachiyomi.source.anime

import android.content.Context
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.source.anime.model.AnimeSourceData
import tachiyomi.domain.source.anime.model.StubAnimeSource
import tachiyomi.domain.source.anime.repository.AnimeSourceDataRepository
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.source.local.entries.anime.LocalAnimeSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap

class AndroidAnimeSourceManager(
    private val context: Context,
    private val extensionManager: AnimeExtensionManager,
    private val sourceRepository: AnimeSourceDataRepository,
) : AnimeSourceManager {
    private val downloadManager: AnimeDownloadManager by injectLazy()

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val sourcesMapFlow = MutableStateFlow(ConcurrentHashMap<Long, AnimeSource>())

    private val stubSourcesMap = ConcurrentHashMap<Long, StubAnimeSource>()

    override val catalogueSources: Flow<List<AnimeCatalogueSource>> = sourcesMapFlow.map { it.values.filterIsInstance<AnimeCatalogueSource>() }

    init {
        scope.launch {
            extensionManager.installedExtensionsFlow
                .collectLatest { extensions ->
                    val mutableMap = ConcurrentHashMap<Long, AnimeSource>(
                        mapOf(
                            LocalAnimeSource.ID to LocalAnimeSource(
                                context,
                                Injekt.get(),
                                Injekt.get(),
                            ),
                        ),
                    )
                    extensions.forEach { extension ->
                        extension.sources.forEach {
                            mutableMap[it.id] = it
                            registerStubSource(it.toSourceData())
                        }
                    }
                    sourcesMapFlow.value = mutableMap
                }
        }

        scope.launch {
            sourceRepository.subscribeAllAnime()
                .collectLatest { sources ->
                    val mutableMap = stubSourcesMap.toMutableMap()
                    sources.forEach {
                        mutableMap[it.id] = StubAnimeSource(it)
                    }
                }
        }
    }

    override fun get(sourceKey: Long): AnimeSource? {
        return sourcesMapFlow.value[sourceKey]
    }

    override fun getOrStub(sourceKey: Long): AnimeSource {
        return sourcesMapFlow.value[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey) }
        }
    }

    override fun getOnlineSources() = sourcesMapFlow.value.values.filterIsInstance<AnimeHttpSource>()

    override fun getCatalogueSources() = sourcesMapFlow.value.values.filterIsInstance<AnimeCatalogueSource>()

    override fun getStubSources(): List<StubAnimeSource> {
        val onlineSourceIds = getOnlineSources().map { it.id }
        return stubSourcesMap.values.filterNot { it.id in onlineSourceIds }
    }

    private fun registerStubSource(sourceData: AnimeSourceData) {
        scope.launch {
            val (id, lang, name) = sourceData
            val dbSourceData = sourceRepository.getAnimeSourceData(id)
            if (dbSourceData == sourceData) return@launch
            sourceRepository.upsertAnimeSourceData(id, lang, name)
            if (dbSourceData != null) {
                downloadManager.renameSource(
                    StubAnimeSource(dbSourceData),
                    StubAnimeSource(sourceData),
                )
            }
        }
    }

    private suspend fun createStubSource(id: Long): StubAnimeSource {
        sourceRepository.getAnimeSourceData(id)?.let {
            return StubAnimeSource(it)
        }
        return StubAnimeSource(AnimeSourceData(id, "", ""))
    }
}
