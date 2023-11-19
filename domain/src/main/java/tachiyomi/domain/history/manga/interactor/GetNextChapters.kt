package tachiyomi.domain.history.manga.interactor

import tachiyomi.domain.entries.manga.interactor.GetManga
import tachiyomi.domain.history.manga.repository.MangaHistoryRepository
import tachiyomi.domain.items.chapter.interactor.GetChapterByMangaId
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.chapter.service.getChapterSort
import kotlin.math.max

class GetNextChapters(
    private val getChapterByMangaId: GetChapterByMangaId,
    private val getManga: GetManga,
    private val historyRepository: MangaHistoryRepository,
) {

    suspend fun await(onlyUnread: Boolean = true): List<Chapter> {
        val history = historyRepository.getLastMangaHistory() ?: return emptyList()
        return await(history.mangaId, history.chapterId, onlyUnread)
    }

    suspend fun await(mangaId: Long, onlyUnread: Boolean = true): List<Chapter> {
        val manga = getManga.await(mangaId) ?: return emptyList()
        val chapters = getChapterByMangaId.await(mangaId)
            .sortedWith(getChapterSort(manga, sortDescending = false))

        return if (onlyUnread) {
            chapters.filterNot { it.read }
        } else {
            chapters
        }
    }

    suspend fun await(mangaId: Long, fromChapterId: Long, onlyUnread: Boolean = true): List<Chapter> {
        val chapters = await(mangaId, onlyUnread)
        val currChapterIndex = chapters.indexOfFirst { it.id == fromChapterId }
        val nextChapters = chapters.subList(max(0, currChapterIndex), chapters.size)

        if (onlyUnread) {
            return nextChapters
        }

        // The "next chapter" is either:
        // - The current chapter if it isn't completely read
        // - The chapters after the current chapter if the current one is completely read
        val fromChapter = chapters.getOrNull(currChapterIndex)
        return if (fromChapter != null && !fromChapter.read) {
            nextChapters
        } else {
            nextChapters.drop(1)
        }
    }
}
