package tachiyomi.domain.source.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.anime.model.AnimeSourceData

interface AnimeSourceDataRepository {
    fun subscribeAllAnime(): Flow<List<AnimeSourceData>>

    suspend fun getAnimeSourceData(id: Long): AnimeSourceData?

    suspend fun upsertAnimeSourceData(id: Long, lang: String, name: String)
}
