package eu.kanade.data.history.manga

import eu.kanade.data.handlers.manga.MangaDatabaseHandler
import eu.kanade.domain.history.manga.model.MangaHistoryUpdate
import eu.kanade.domain.history.manga.model.MangaHistoryWithRelations
import eu.kanade.domain.history.manga.repository.MangaHistoryRepository
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority

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
