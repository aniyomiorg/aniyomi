package eu.kanade.domain.items.chapter.interactor

import eu.kanade.domain.items.chapter.model.ChapterUpdate
import eu.kanade.domain.items.chapter.repository.ChapterRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class UpdateChapter(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(chapterUpdate: ChapterUpdate) {
        try {
            chapterRepository.updateChapter(chapterUpdate)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitAll(chapterUpdates: List<ChapterUpdate>) {
        try {
            chapterRepository.updateAllChapters(chapterUpdates)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
