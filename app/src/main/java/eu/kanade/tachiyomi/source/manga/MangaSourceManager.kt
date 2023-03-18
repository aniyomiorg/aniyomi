package eu.kanade.tachiyomi.source.manga

import android.content.Context
import eu.kanade.domain.source.manga.model.MangaSourceData
import eu.kanade.domain.source.manga.repository.MangaSourceDataRepository
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.MangaSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
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
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap

class MangaSourceManager(
    private val context: Context,
    private val extensionManager: MangaExtensionManager,
    private val sourceRepository: MangaSourceDataRepository,
) {
    private val downloadManager: MangaDownloadManager by injectLazy()

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val sourcesMapFlow = MutableStateFlow(ConcurrentHashMap<Long, MangaSource>())

    private val stubSourcesMap = ConcurrentHashMap<Long, StubMangaSource>()

    val catalogueSources: Flow<List<CatalogueSource>> = sourcesMapFlow.map { it.values.filterIsInstance<CatalogueSource>() }
    val onlineSources: Flow<List<HttpSource>> = catalogueSources.map { sources -> sources.filterIsInstance<HttpSource>() }

    init {
        scope.launch {
            extensionManager.installedExtensionsFlow
                .collectLatest { extensions ->
                    val mutableMap = ConcurrentHashMap<Long, MangaSource>(mapOf(LocalMangaSource.ID to LocalMangaSource(context)))
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

    fun get(sourceKey: Long): MangaSource? {
        return sourcesMapFlow.value[sourceKey]
    }

    fun getOrStub(sourceKey: Long): MangaSource {
        return sourcesMapFlow.value[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey) }
        }
    }

    fun getOnlineSources() = sourcesMapFlow.value.values.filterIsInstance<HttpSource>()

    fun getCatalogueSources() = sourcesMapFlow.value.values.filterIsInstance<CatalogueSource>()

    fun getStubSources(): List<StubMangaSource> {
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

    @Suppress("OverridingDeprecatedMember")
    open inner class StubMangaSource(private val sourceData: MangaSourceData) : MangaSource {

        override val id: Long = sourceData.id

        override val name: String = sourceData.name.ifBlank { id.toString() }

        override val lang: String = sourceData.lang

        override suspend fun getMangaDetails(manga: SManga): SManga {
            throw getSourceNotInstalledException()
        }

        @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getMangaDetails"))
        override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
            return Observable.error(getSourceNotInstalledException())
        }

        override suspend fun getChapterList(manga: SManga): List<SChapter> {
            throw getSourceNotInstalledException()
        }

        @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getChapterList"))
        override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
            return Observable.error(getSourceNotInstalledException())
        }

        override suspend fun getPageList(chapter: SChapter): List<Page> {
            throw getSourceNotInstalledException()
        }

        @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getPageList"))
        override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
            return Observable.error(getSourceNotInstalledException())
        }

        override fun toString(): String {
            return if (sourceData.isMissingInfo.not()) "$name (${lang.uppercase()})" else id.toString()
        }

        fun getSourceNotInstalledException(): SourceNotInstalledException {
            return SourceNotInstalledException(toString())
        }
    }

    inner class SourceNotInstalledException(val sourceString: String) :
        Exception(context.getString(R.string.source_not_installed, sourceString))
}
