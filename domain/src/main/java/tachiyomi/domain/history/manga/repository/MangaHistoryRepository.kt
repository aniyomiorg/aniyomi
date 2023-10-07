package tachiyomi.domain.history.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.manga.model.MangaHistoryUpdate
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations

interface MangaHistoryRepository {

    fun getMangaHistory(query: String): Flow<List<MangaHistoryWithRelations>>

    suspend fun getLastMangaHistory(): MangaHistoryWithRelations?

    suspend fun getTotalReadDuration(): Long

    suspend fun resetMangaHistory(historyId: Long)

    suspend fun resetHistoryByMangaId(mangaId: Long)

    suspend fun deleteAllMangaHistory(): Boolean

    suspend fun upsertMangaHistory(historyUpdate: MangaHistoryUpdate)
}
