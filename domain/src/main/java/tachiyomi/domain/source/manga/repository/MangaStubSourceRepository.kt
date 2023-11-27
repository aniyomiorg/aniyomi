package tachiyomi.domain.source.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.manga.model.StubMangaSource

interface MangaStubSourceRepository {
    fun subscribeAllManga(): Flow<List<StubMangaSource>>

    suspend fun getStubMangaSource(id: Long): StubMangaSource?

    suspend fun upsertStubMangaSource(id: Long, lang: String, name: String)
}
