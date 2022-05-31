package eu.kanade.tachiyomi.animesource

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import rx.Observable
import tachiyomi.animesource.model.AnimeInfo
import tachiyomi.animesource.model.EpisodeInfo

class AnimeSourceManager(private val context: Context) {

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
            StubSource(sourceKey)
        }
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance<AnimeHttpSource>()

    fun getCatalogueSources() = sourcesMap.values.filterIsInstance<AnimeCatalogueSource>()

    internal fun registerSource(source: AnimeSource) {
        if (!sourcesMap.containsKey(source.id)) {
            sourcesMap[source.id] = source
        }
        if (!stubSourcesMap.containsKey(source.id)) {
            stubSourcesMap[source.id] = StubSource(source.id)
        }
        triggerCatalogueSources()
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

    @Suppress("OverridingDeprecatedMember")
    inner class StubSource(override val id: Long) : AnimeSource {

        override val name: String
            get() = id.toString()

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
            return name
        }

        private fun getSourceNotInstalledException(): SourceNotInstalledException {
            return SourceNotInstalledException(id)
        }
    }

    inner class SourceNotInstalledException(val id: Long) :
        Exception(context.getString(R.string.source_not_installed, id.toString()))
}
