package tachiyomi.data.items.chapter

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.lang.toLong
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.chapter.model.ChapterUpdate
import tachiyomi.domain.items.chapter.repository.ChapterRepository

class ChapterRepositoryImpl(
    private val handler: MangaDatabaseHandler,
) : ChapterRepository {

    override suspend fun addAllChapters(chapters: List<Chapter>): List<Chapter> {
        return try {
            handler.await(inTransaction = true) {
                chapters.map { chapter ->
                    chaptersQueries.insert(
                        chapter.mangaId,
                        chapter.url,
                        chapter.name,
                        chapter.scanlator,
                        chapter.read,
                        chapter.bookmark,
                        chapter.lastPageRead,
                        chapter.chapterNumber,
                        chapter.sourceOrder,
                        chapter.dateFetch,
                        chapter.dateUpload,
                        chapter.version,
                    )
                    val lastInsertId = chaptersQueries.selectLastInsertedRowId().executeAsOne()
                    chapter.copy(id = lastInsertId)
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    override suspend fun updateChapter(chapterUpdate: ChapterUpdate) {
        partialUpdate(chapterUpdate)
    }

    override suspend fun updateAllChapters(chapterUpdates: List<ChapterUpdate>) {
        partialUpdate(*chapterUpdates.toTypedArray())
    }

    private suspend fun partialUpdate(vararg chapterUpdates: ChapterUpdate) {
        handler.await(inTransaction = true) {
            chapterUpdates.forEach { chapterUpdate ->
                chaptersQueries.update(
                    mangaId = chapterUpdate.mangaId,
                    url = chapterUpdate.url,
                    name = chapterUpdate.name,
                    scanlator = chapterUpdate.scanlator,
                    read = chapterUpdate.read,
                    bookmark = chapterUpdate.bookmark,
                    lastPageRead = chapterUpdate.lastPageRead,
                    chapterNumber = chapterUpdate.chapterNumber,
                    sourceOrder = chapterUpdate.sourceOrder,
                    dateFetch = chapterUpdate.dateFetch,
                    dateUpload = chapterUpdate.dateUpload,
                    chapterId = chapterUpdate.id,
                    version = chapterUpdate.version,
                    isSyncing = 0,
                )
            }
        }
    }

    override suspend fun removeChaptersWithIds(chapterIds: List<Long>) {
        try {
            handler.await { chaptersQueries.removeChaptersWithIds(chapterIds) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    override suspend fun getChapterByMangaId(mangaId: Long, applyScanlatorFilter: Boolean): List<Chapter> {
        return handler.awaitList {
            chaptersQueries.getChaptersByMangaId(mangaId, applyScanlatorFilter.toLong(), ::mapChapter)
        }
    }

    override suspend fun getScanlatorsByMangaId(mangaId: Long): List<String> {
        return handler.awaitList {
            chaptersQueries.getScanlatorsByMangaId(mangaId) { it.orEmpty() }
        }
    }

    override fun getScanlatorsByMangaIdAsFlow(mangaId: Long): Flow<List<String>> {
        return handler.subscribeToList {
            chaptersQueries.getScanlatorsByMangaId(mangaId) { it.orEmpty() }
        }
    }

    override suspend fun getBookmarkedChaptersByMangaId(mangaId: Long): List<Chapter> {
        return handler.awaitList {
            chaptersQueries.getBookmarkedChaptersByMangaId(
                mangaId,
                ::mapChapter,
            )
        }
    }

    override suspend fun getChapterById(id: Long): Chapter? {
        return handler.awaitOneOrNull { chaptersQueries.getChapterById(id, ::mapChapter) }
    }

    override suspend fun getChapterByMangaIdAsFlow(mangaId: Long, applyScanlatorFilter: Boolean): Flow<List<Chapter>> {
        return handler.subscribeToList {
            chaptersQueries.getChaptersByMangaId(mangaId, applyScanlatorFilter.toLong(), ::mapChapter)
        }
    }

    override suspend fun getChapterByUrlAndMangaId(url: String, mangaId: Long): Chapter? {
        return handler.awaitOneOrNull {
            chaptersQueries.getChapterByUrlAndMangaId(
                url,
                mangaId,
                ::mapChapter,
            )
        }
    }

    private fun mapChapter(
        id: Long,
        mangaId: Long,
        url: String,
        name: String,
        scanlator: String?,
        read: Boolean,
        bookmark: Boolean,
        lastPageRead: Long,
        chapterNumber: Double,
        sourceOrder: Long,
        dateFetch: Long,
        dateUpload: Long,
        lastModifiedAt: Long,
        version: Long,
        @Suppress("UNUSED_PARAMETER")
        isSyncing: Long,
    ): Chapter = Chapter(
        id = id,
        mangaId = mangaId,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        dateFetch = dateFetch,
        sourceOrder = sourceOrder,
        url = url,
        name = name,
        dateUpload = dateUpload,
        chapterNumber = chapterNumber,
        scanlator = scanlator,
        lastModifiedAt = lastModifiedAt,
        version = version,
    )
}
