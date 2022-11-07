package eu.kanade.domain.animesource.repository

import eu.kanade.domain.animesource.model.AnimeSourceData
import kotlinx.coroutines.flow.Flow

interface AnimeSourceDataRepository {
    fun subscribeAll(): Flow<List<AnimeSourceData>>

    suspend fun getSourceData(id: Long): AnimeSourceData?

    suspend fun upsertSourceData(id: Long, lang: String, name: String)
}
