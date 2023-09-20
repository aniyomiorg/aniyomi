package tachiyomi.domain.items.chapter.interactor

import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.chapter.repository.ChapterRepository

class GetChapterByMangaId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(mangaId: Long): List<Chapter> {
        return try {
            chapterRepository.getChapterByMangaId(mangaId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
