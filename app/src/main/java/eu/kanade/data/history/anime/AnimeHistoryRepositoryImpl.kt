package eu.kanade.data.history.anime

import eu.kanade.data.handlers.anime.AnimeDatabaseHandler
import eu.kanade.domain.history.anime.model.AnimeHistoryUpdate
import eu.kanade.domain.history.anime.model.AnimeHistoryWithRelations
import eu.kanade.domain.history.anime.repository.AnimeHistoryRepository
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority

class AnimeHistoryRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeHistoryRepository {

    override fun getAnimeHistory(query: String): Flow<List<AnimeHistoryWithRelations>> {
        return handler.subscribeToList {
            animehistoryViewQueries.animehistory(query, animeHistoryWithRelationsMapper)
        }
    }

    override suspend fun getLastAnimeHistory(): AnimeHistoryWithRelations? {
        return handler.awaitOneOrNull {
            animehistoryViewQueries.getLatestAnimeHistory(animeHistoryWithRelationsMapper)
        }
    }

    override suspend fun resetAnimeHistory(historyId: Long) {
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

    override suspend fun deleteAllAnimeHistory(): Boolean {
        return try {
            handler.await { animehistoryQueries.removeAllHistory() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
            false
        }
    }

    override suspend fun upsertAnimeHistory(historyUpdate: AnimeHistoryUpdate) {
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
