package eu.kanade.domain.animehistory.repository

import eu.kanade.domain.animehistory.model.AnimeHistoryUpdate
import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import kotlinx.coroutines.flow.Flow

interface AnimeHistoryRepository {

    fun getAnimeHistory(query: String): Flow<List<AnimeHistoryWithRelations>>

    suspend fun getLastAnimeHistory(): AnimeHistoryWithRelations?

    suspend fun resetAnimeHistory(historyId: Long)

    suspend fun resetHistoryByAnimeId(animeId: Long)

    suspend fun deleteAllAnimeHistory(): Boolean

    suspend fun upsertAnimeHistory(historyUpdate: AnimeHistoryUpdate)
}
