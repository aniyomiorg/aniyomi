package tachiyomi.domain.source.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.manga.model.MangaSourceData

interface MangaSourceDataRepository {
    fun subscribeAllManga(): Flow<List<MangaSourceData>>

    suspend fun getMangaSourceData(id: Long): MangaSourceData?

    suspend fun upsertMangaSourceData(id: Long, lang: String, name: String)
}
