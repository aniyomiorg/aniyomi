package eu.kanade.domain.animesource.repository

import eu.kanade.domain.animesource.model.AnimeSourceData
import kotlinx.coroutines.flow.Flow

interface AnimeSourceDataRepository {
    fun subscribeAllAnime(): Flow<List<AnimeSourceData>>

    suspend fun getAnimeSourceData(id: Long): AnimeSourceData?

    suspend fun upsertAnimeSourceData(id: Long, lang: String, name: String)
}
