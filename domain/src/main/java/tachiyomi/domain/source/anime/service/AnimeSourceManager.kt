package tachiyomi.domain.source.anime.service

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.source.anime.model.StubAnimeSource

interface AnimeSourceManager {

    val isInitialized: StateFlow<Boolean>

    val sources: Flow<List<AnimeSource>>

    fun get(sourceKey: Long): AnimeSource?

    fun getOrStub(sourceKey: Long): AnimeSource

    fun getAll(): List<AnimeSource>

    fun getOnlineSources(): List<AnimeHttpSource>

    fun getStubSources(): List<StubAnimeSource>
}
