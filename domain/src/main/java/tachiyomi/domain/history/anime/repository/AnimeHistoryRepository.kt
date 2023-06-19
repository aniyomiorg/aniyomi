package tachiyomi.domain.history.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.anime.model.AnimeHistoryUpdate
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations

interface AnimeHistoryRepository {

    fun getAnimeHistory(query: String): Flow<List<AnimeHistoryWithRelations>>

    suspend fun getLastAnimeHistory(): AnimeHistoryWithRelations?

    suspend fun resetAnimeHistory(historyId: Long)

    suspend fun resetHistoryByAnimeId(animeId: Long)

    suspend fun deleteAllAnimeHistory(): Boolean

    suspend fun upsertAnimeHistory(historyUpdate: AnimeHistoryUpdate)
}
