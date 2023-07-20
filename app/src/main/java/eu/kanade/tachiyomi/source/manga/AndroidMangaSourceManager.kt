package eu.kanade.tachiyomi.source.manga

import android.content.Context
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.MangaSource
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.source.manga.model.MangaSourceData
import tachiyomi.domain.source.manga.model.StubMangaSource
import tachiyomi.domain.source.manga.repository.MangaSourceDataRepository
import tachiyomi.domain.source.manga.service.MangaSourceManager
import tachiyomi.source.local.entries.manga.LocalMangaSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap

class AndroidMangaSourceManager(
    private val context: Context,
    private val extensionManager: MangaExtensionManager,
    private val sourceRepository: MangaSourceDataRepository,
) : MangaSourceManager {
    private val downloadManager: MangaDownloadManager by injectLazy()

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val sourcesMapFlow = MutableStateFlow(ConcurrentHashMap<Long, MangaSource>())

    private val stubSourcesMap = ConcurrentHashMap<Long, StubMangaSource>()

    override val catalogueSources: Flow<List<CatalogueSource>> = sourcesMapFlow.map { it.values.filterIsInstance<CatalogueSource>() }

    init {
        scope.launch {
            extensionManager.installedExtensionsFlow
                .collectLatest { extensions ->
                    val mutableMap = ConcurrentHashMap<Long, MangaSource>(
                        mapOf(
                            LocalMangaSource.ID to LocalMangaSource(
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
            sourceRepository.subscribeAllManga()
                .collectLatest { sources ->
                    val mutableMap = stubSourcesMap.toMutableMap()
                    sources.forEach {
                        mutableMap[it.id] = StubMangaSource(it)
                    }
                }
        }
    }

    override fun get(sourceKey: Long): MangaSource? {
        return sourcesMapFlow.value[sourceKey]
    }

    override fun getOrStub(sourceKey: Long): MangaSource {
        return sourcesMapFlow.value[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey) }
        }
    }

    override fun getOnlineSources() = sourcesMapFlow.value.values.filterIsInstance<HttpSource>()

    override fun getCatalogueSources() = sourcesMapFlow.value.values.filterIsInstance<CatalogueSource>()

    override fun getStubSources(): List<StubMangaSource> {
        val onlineSourceIds = getOnlineSources().map { it.id }
        return stubSourcesMap.values.filterNot { it.id in onlineSourceIds }
    }

    private fun registerStubSource(sourceData: MangaSourceData) {
        scope.launch {
            val (id, lang, name) = sourceData
            val dbSourceData = sourceRepository.getMangaSourceData(id)
            if (dbSourceData == sourceData) return@launch
            sourceRepository.upsertMangaSourceData(id, lang, name)
            if (dbSourceData != null) {
                downloadManager.renameSource(StubMangaSource(dbSourceData), StubMangaSource(sourceData))
            }
        }
    }

    private suspend fun createStubSource(id: Long): StubMangaSource {
        sourceRepository.getMangaSourceData(id)?.let {
            return StubMangaSource(it)
        }
        extensionManager.getSourceData(id)?.let {
            registerStubSource(it)
            return StubMangaSource(it)
        }
        return StubMangaSource(MangaSourceData(id, "", ""))
    }
}
