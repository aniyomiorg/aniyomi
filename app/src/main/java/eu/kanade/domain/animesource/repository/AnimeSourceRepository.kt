package eu.kanade.domain.animesource.repository

import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.animesource.model.AnimeSourceData
import kotlinx.coroutines.flow.Flow
import eu.kanade.tachiyomi.animesource.AnimeSource as LoadedAnimeSource

interface AnimeSourceRepository {

    fun getSources(): Flow<List<AnimeSource>>

    fun getOnlineSources(): Flow<List<AnimeSource>>

    fun getSourcesWithFavoriteCount(): Flow<List<Pair<AnimeSource, Long>>>

    fun getSourcesWithNonLibraryAnime(): Flow<List<Pair<LoadedAnimeSource, Long>>>

    suspend fun getAnimeSourceData(id: Long): AnimeSourceData?

    suspend fun upsertAnimeSourceData(id: Long, lang: String, name: String)
}
