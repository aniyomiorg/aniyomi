package eu.kanade.data.animehistory

import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.domain.animehistory.model.AnimeHistoryUpdate
import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority

class AnimeHistoryRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeHistoryRepository {

    override fun getHistory(query: String): Flow<List<AnimeHistoryWithRelations>> {
        return handler.subscribeToList {
            animehistoryViewQueries.animehistory(query, animehistoryWithRelationsMapper)
        }
    }

    override suspend fun getLastHistory(): AnimeHistoryWithRelations? {
        return handler.awaitOneOrNull {
            animehistoryViewQueries.getLatestAnimeHistory(animehistoryWithRelationsMapper)
        }
    }

    override suspend fun resetHistory(historyId: Long) {
        try {
            handler.await { animehistoryQueries.resetAnimeHistoryById(historyId) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun resetHistoryByAnimeId(animeId: Long) {
        try {
            handler.await { animehistoryQueries.resetHistoryByAnimeId(animeId) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun deleteAllHistory(): Boolean {
        return try {
            handler.await { animehistoryQueries.removeAllHistory() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
            false
        }
    }

    override suspend fun upsertHistory(historyUpdate: AnimeHistoryUpdate) {
        try {
            handler.await {
                animehistoryQueries.upsert(
                    historyUpdate.episodeId,
                    historyUpdate.seenAt,
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }
}
