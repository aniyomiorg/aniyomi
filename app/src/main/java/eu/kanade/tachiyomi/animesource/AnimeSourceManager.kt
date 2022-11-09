package eu.kanade.tachiyomi.animesource

import android.content.Context
import eu.kanade.domain.animesource.model.AnimeSourceData
import eu.kanade.domain.animesource.repository.AnimeSourceDataRepository
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animeextension.AnimeExtensionManager
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap

class AnimeSourceManager(
    private val context: Context,
    private val extensionManager: AnimeExtensionManager,
    private val sourceRepository: AnimeSourceDataRepository,
) {
    private val downloadManager: AnimeDownloadManager by injectLazy()

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val sourcesMapFlow = MutableStateFlow(ConcurrentHashMap<Long, AnimeSource>())

    private val stubSourcesMap = ConcurrentHashMap<Long, StubAnimeSource>()

    val catalogueSources: Flow<List<AnimeCatalogueSource>> = sourcesMapFlow.map { it.values.filterIsInstance<AnimeCatalogueSource>() }
    val onlineSources: Flow<List<AnimeHttpSource>> = catalogueSources.map { sources -> sources.filterIsInstance<AnimeHttpSource>() }

    init {
        scope.launch {
            extensionManager.installedExtensionsFlow
                .collectLatest { extensions ->
                    val mutableMap = ConcurrentHashMap<Long, AnimeSource>(mapOf(LocalAnimeSource.ID to LocalAnimeSource(context)))
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
            sourceRepository.subscribeAll()
                .collectLatest { sources ->
                    val mutableMap = stubSourcesMap.toMutableMap()
                    sources.forEach {
                        mutableMap[it.id] = StubAnimeSource(it)
                    }
                }
        }
    }

    fun get(sourceKey: Long): AnimeSource? {
        return sourcesMapFlow.value[sourceKey]
    }

    fun getOrStub(sourceKey: Long): AnimeSource {
        return sourcesMapFlow.value[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey) }
        }
    }

    fun getOnlineSources() = sourcesMapFlow.value.values.filterIsInstance<AnimeHttpSource>()

    fun getCatalogueSources() = sourcesMapFlow.value.values.filterIsInstance<AnimeCatalogueSource>()

    fun getStubSources(): List<StubAnimeSource> {
        val onlineSourceIds = getOnlineSources().map { it.id }
        return stubSourcesMap.values.filterNot { it.id in onlineSourceIds }
    }

    private fun registerStubSource(sourceData: AnimeSourceData) {
        scope.launch {
            val (id, lang, name) = sourceData
            val dbSourceData = sourceRepository.getSourceData(id)
            if (dbSourceData == sourceData) return@launch
            sourceRepository.upsertSourceData(id, lang, name)
            if (dbSourceData != null) {
                downloadManager.renameSource(
                    StubAnimeSource(dbSourceData),
                    StubAnimeSource(sourceData),
                )
            }
        }
    }

    private suspend fun createStubSource(id: Long): StubAnimeSource {
        sourceRepository.getSourceData(id)?.let {
            return StubAnimeSource(it)
        }
        return StubAnimeSource(AnimeSourceData(id, "", ""))
    }

    @Suppress("OverridingDeprecatedMember")
    open inner class StubAnimeSource(private val sourceData: AnimeSourceData) : AnimeSource {

        override val id: Long = sourceData.id

        override val name: String = sourceData.name.ifBlank { id.toString() }

        override val lang: String = sourceData.lang

        override suspend fun getAnimeDetails(anime: SAnime): SAnime {
            throw getSourceNotInstalledException()
        }

        override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
            throw getSourceNotInstalledException()
        }

        override suspend fun getVideoList(episode: SEpisode): List<Video> {
            throw getSourceNotInstalledException()
        }

        override fun toString(): String {
            return if (sourceData.isMissingInfo.not()) "$name (${lang.uppercase()})" else id.toString()
        }

        fun getSourceNotInstalledException(): AnimeSourceNotInstalledException {
            return AnimeSourceNotInstalledException(toString())
        }
    }

    inner class AnimeSourceNotInstalledException(val sourceString: String) :
        Exception(context.getString(R.string.source_not_installed, sourceString))
}
