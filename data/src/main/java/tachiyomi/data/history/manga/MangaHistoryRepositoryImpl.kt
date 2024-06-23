package tachiyomi.data.history.manga

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.history.manga.model.MangaHistory
import tachiyomi.domain.history.manga.model.MangaHistoryUpdate
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.domain.history.manga.repository.MangaHistoryRepository

class MangaHistoryRepositoryImpl(
    private val handler: MangaDatabaseHandler,
) : MangaHistoryRepository {

    override fun getMangaHistory(query: String): Flow<List<MangaHistoryWithRelations>> {
        return handler.subscribeToList {
            historyViewQueries.history(query, MangaHistoryMapper::mapMangaHistoryWithRelations)
        }
    }

    override suspend fun getLastMangaHistory(): MangaHistoryWithRelations? {
        return handler.awaitOneOrNull {
            historyViewQueries.getLatestHistory(MangaHistoryMapper::mapMangaHistoryWithRelations)
        }
    }

    override suspend fun getTotalReadDuration(): Long {
        return handler.awaitOne { historyQueries.getReadDuration() }
    }

    override suspend fun getHistoryByMangaId(mangaId: Long): List<MangaHistory> {
        return handler.awaitList { historyQueries.getHistoryByMangaId(mangaId, MangaHistoryMapper::mapMangaHistory) }
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
