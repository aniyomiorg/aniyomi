package eu.kanade.domain.animehistory.repository

import androidx.paging.PagingSource
import eu.kanade.domain.animehistory.model.AnimeHistoryUpdate
import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import eu.kanade.domain.episode.model.Episode

interface AnimeHistoryRepository {

    fun getHistory(query: String): PagingSource<Long, AnimeHistoryWithRelations>

    suspend fun getLastHistory(): AnimeHistoryWithRelations?

    suspend fun getNextEpisode(animeId: Long, episodeId: Long): Episode?

    suspend fun resetHistory(historyId: Long)

    suspend fun resetHistoryByAnimeId(animeId: Long)

    suspend fun deleteAllHistory(): Boolean

    suspend fun upsertHistory(historyUpdate: AnimeHistoryUpdate)
}
