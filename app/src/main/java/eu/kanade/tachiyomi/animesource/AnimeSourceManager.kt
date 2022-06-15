package eu.kanade.tachiyomi.animesource

import android.content.Context
import eu.kanade.domain.animesource.interactor.GetAnimeSourceData
import eu.kanade.domain.animesource.interactor.UpsertAnimeSourceData
import eu.kanade.domain.animesource.model.AnimeSourceData
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.extension.AnimeExtensionManager
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import rx.Observable
import tachiyomi.animesource.model.AnimeInfo
import tachiyomi.animesource.model.EpisodeInfo
import uy.kohesive.injekt.injectLazy

class AnimeSourceManager(private val context: Context) {

    private val extensionManager: AnimeExtensionManager by injectLazy()
    private val getSourceData: GetAnimeSourceData by injectLazy()
    private val upsertSourceData: UpsertAnimeSourceData by injectLazy()

    private val sourcesMap = mutableMapOf<Long, AnimeSource>()
    private val stubSourcesMap = mutableMapOf<Long, StubSource>()

    private val _catalogueSources: MutableStateFlow<List<AnimeCatalogueSource>> = MutableStateFlow(listOf())
    val catalogueSources: Flow<List<AnimeCatalogueSource>> = _catalogueSources
    val onlineSources: Flow<List<AnimeHttpSource>> =
        _catalogueSources.map { sources -> sources.filterIsInstance<AnimeHttpSource>() }

    init {
        createInternalSources().forEach { registerSource(it) }
    }

    fun get(sourceKey: Long): AnimeSource? {
        return sourcesMap[sourceKey]
    }

    fun getOrStub(sourceKey: Long): AnimeSource {
        return sourcesMap[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey) }
        }
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance<AnimeHttpSource>()

    fun getCatalogueSources() = sourcesMap.values.filterIsInstance<AnimeCatalogueSource>()

    fun getStubSources(): List<StubSource> {
        val onlineSourceIds = getOnlineSources().map { it.id }
        return stubSourcesMap.values.filterNot { it.id in onlineSourceIds }
    }

    internal fun registerSource(source: AnimeSource) {
        if (!sourcesMap.containsKey(source.id)) {
            sourcesMap[source.id] = source
        }
        registerStubSource(source.toAnimeSourceData())
        triggerCatalogueSources()
    }

    private fun registerStubSource(sourceData: AnimeSourceData) {
        launchIO {
            val dbSourceData = getSourceData.await(sourceData.id)

            if (dbSourceData != sourceData) {
                upsertSourceData.await(sourceData)
            }
            if (stubSourcesMap[sourceData.id]?.toAnimeSourceData() != sourceData) {
                stubSourcesMap[sourceData.id] = StubSource(sourceData)
            }
        }
    }

    internal fun unregisterSource(source: AnimeSource) {
        sourcesMap.remove(source.id)
        triggerCatalogueSources()
    }

    private fun triggerCatalogueSources() {
        _catalogueSources.update {
            sourcesMap.values.filterIsInstance<AnimeCatalogueSource>()
        }
    }

    private fun createInternalSources(): List<AnimeSource> = listOf(
        LocalAnimeSource(context),
    )

    private suspend fun createStubSource(id: Long): StubSource {
        getSourceData.await(id)?.let {
            return StubSource(it)
        }
        return StubSource(AnimeSourceData(id, "", ""))
    }

    @Suppress("OverridingDeprecatedMember")
    open inner class StubSource(val sourceData: AnimeSourceData) : AnimeSource {

        override val name: String = sourceData.name

        override val lang: String = sourceData.lang

        override val id: Long = sourceData.id

        override fun fetchAnimeDetails(anime: SAnime): Observable<SAnime> {
            return Observable.error(getSourceNotInstalledException())
        }

        override suspend fun getAnimeDetails(anime: AnimeInfo): AnimeInfo {
            throw getSourceNotInstalledException()
        }

        override fun fetchEpisodeList(anime: SAnime): Observable<List<SEpisode>> {
            return Observable.error(getSourceNotInstalledException())
        }

        override suspend fun getEpisodeList(anime: AnimeInfo): List<EpisodeInfo> {
            throw getSourceNotInstalledException()
        }

        override fun fetchVideoList(episode: SEpisode): Observable<List<Video>> {
            return Observable.error(getSourceNotInstalledException())
        }

        override suspend fun getVideoList(episode: EpisodeInfo): List<tachiyomi.animesource.model.Video> {
            throw getSourceNotInstalledException()
        }

        override fun toString(): String {
            if (name.isNotBlank() && lang.isNotBlank()) {
                return "$name (${lang.uppercase()})"
            }
            return id.toString()
        }

        fun getSourceNotInstalledException(): SourceNotInstalledException {
            return SourceNotInstalledException(toString())
        }
    }

    inner class SourceNotInstalledException(val sourceString: String) :
        Exception(context.getString(R.string.source_not_installed, sourceString))
}
