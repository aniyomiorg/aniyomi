package tachiyomi.data.history.manga

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.history.manga.model.MangaHistoryUpdate
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.domain.history.manga.repository.MangaHistoryRepository

class MangaHistoryRepositoryImpl(
    private val handler: MangaDatabaseHandler,
) : MangaHistoryRepository {

    override fun getMangaHistory(query: String): Flow<List<MangaHistoryWithRelations>> {
        return handler.subscribeToList {
            historyViewQueries.history(query, mangaHistoryWithRelationsMapper)
        }
    }

    override suspend fun getLastMangaHistory(): MangaHistoryWithRelations? {
        return handler.awaitOneOrNull {
            historyViewQueries.getLatestHistory(mangaHistoryWithRelationsMapper)
        }
    }

    override suspend fun getTotalReadDuration(): Long {
        return handler.awaitOne { historyQueries.getReadDuration() }
    }

    override suspend fun resetMangaHistory(historyId: Long) {
        try {
            handler.await { historyQueries.resetHistoryById(historyId) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun resetHistoryByMangaId(mangaId: Long) {
        try {
            handler.await { historyQueries.resetHistoryByMangaId(mangaId) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun deleteAllMangaHistory(): Boolean {
        return try {
            handler.await { historyQueries.removeAllHistory() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
            false
        }
    }

    override suspend fun upsertMangaHistory(historyUpdate: MangaHistoryUpdate) {
        try {
            handler.await {
                historyQueries.upsert(
                    historyUpdate.chapterId,
                    historyUpdate.readAt,
                    historyUpdate.sessionReadDuration,
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }
}
