package eu.kanade.domain.source.repository

import eu.kanade.domain.source.model.SourceData
import kotlinx.coroutines.flow.Flow

interface SourceDataRepository {
    fun subscribeAllManga(): Flow<List<SourceData>>

    suspend fun getMangaSourceData(id: Long): SourceData?

    suspend fun upsertMangaSourceData(id: Long, lang: String, name: String)
}
