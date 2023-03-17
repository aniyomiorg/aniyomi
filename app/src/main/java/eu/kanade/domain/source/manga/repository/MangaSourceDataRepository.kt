package eu.kanade.domain.source.manga.repository

import eu.kanade.domain.source.manga.model.MangaSourceData
import kotlinx.coroutines.flow.Flow

interface MangaSourceDataRepository {
    fun subscribeAllManga(): Flow<List<MangaSourceData>>

    suspend fun getMangaSourceData(id: Long): MangaSourceData?

    suspend fun upsertMangaSourceData(id: Long, lang: String, name: String)
}
