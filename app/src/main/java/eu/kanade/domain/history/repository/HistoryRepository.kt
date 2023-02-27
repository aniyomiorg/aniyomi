package eu.kanade.domain.history.repository

import eu.kanade.domain.history.model.HistoryUpdate
import eu.kanade.domain.history.model.HistoryWithRelations
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {

    fun getMangaHistory(query: String): Flow<List<HistoryWithRelations>>

    suspend fun getLastMangaHistory(): HistoryWithRelations?

    suspend fun getTotalReadDuration(): Long

    suspend fun resetMangaHistory(historyId: Long)

    suspend fun resetHistoryByMangaId(mangaId: Long)

    suspend fun deleteAllMangaHistory(): Boolean

    suspend fun upsertMangaHistory(historyUpdate: HistoryUpdate)
}
