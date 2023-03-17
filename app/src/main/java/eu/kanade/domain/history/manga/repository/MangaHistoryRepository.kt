package eu.kanade.domain.history.manga.repository

import eu.kanade.domain.history.manga.model.MangaHistoryUpdate
import eu.kanade.domain.history.manga.model.MangaHistoryWithRelations
import kotlinx.coroutines.flow.Flow

interface MangaHistoryRepository {

    fun getMangaHistory(query: String): Flow<List<MangaHistoryWithRelations>>

    suspend fun getLastMangaHistory(): MangaHistoryWithRelations?

    suspend fun getTotalReadDuration(): Long

    suspend fun resetMangaHistory(historyId: Long)

    suspend fun resetHistoryByMangaId(mangaId: Long)

    suspend fun deleteAllMangaHistory(): Boolean

    suspend fun upsertMangaHistory(historyUpdate: MangaHistoryUpdate)
}
