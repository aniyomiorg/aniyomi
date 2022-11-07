package eu.kanade.domain.animehistory.repository

import eu.kanade.domain.animehistory.model.AnimeHistoryUpdate
import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import kotlinx.coroutines.flow.Flow

interface AnimeHistoryRepository {

    fun getHistory(query: String): Flow<List<AnimeHistoryWithRelations>>

    suspend fun getLastHistory(): AnimeHistoryWithRelations?

    suspend fun resetHistory(historyId: Long)

    suspend fun resetHistoryByAnimeId(animeId: Long)

    suspend fun deleteAllHistory(): Boolean

    suspend fun upsertHistory(historyUpdate: AnimeHistoryUpdate)
}
