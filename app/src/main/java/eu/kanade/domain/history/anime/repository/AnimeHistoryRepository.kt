package eu.kanade.domain.history.anime.repository

import eu.kanade.domain.history.anime.model.AnimeHistoryUpdate
import eu.kanade.domain.history.anime.model.AnimeHistoryWithRelations
import kotlinx.coroutines.flow.Flow

interface AnimeHistoryRepository {

    fun getAnimeHistory(query: String): Flow<List<AnimeHistoryWithRelations>>

    suspend fun getLastAnimeHistory(): AnimeHistoryWithRelations?

    suspend fun resetAnimeHistory(historyId: Long)

    suspend fun resetHistoryByAnimeId(animeId: Long)

    suspend fun deleteAllAnimeHistory(): Boolean

    suspend fun upsertAnimeHistory(historyUpdate: AnimeHistoryUpdate)
}
