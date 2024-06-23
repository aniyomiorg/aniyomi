package eu.kanade.domain.track.manga.interactor

import eu.kanade.domain.track.manga.model.toDbTrack
import eu.kanade.tachiyomi.data.track.EnhancedMangaTracker
import eu.kanade.tachiyomi.data.track.MangaTracker
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.items.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.items.chapter.interactor.UpdateChapter
import tachiyomi.domain.items.chapter.model.toChapterUpdate
import tachiyomi.domain.track.manga.interactor.InsertMangaTrack
import tachiyomi.domain.track.manga.model.MangaTrack

class SyncChapterProgressWithTrack(
    private val updateChapter: UpdateChapter,
    private val insertTrack: InsertMangaTrack,
    private val getChaptersByMangaId: GetChaptersByMangaId,
) {

    suspend fun await(
        mangaId: Long,
        remoteTrack: MangaTrack,
        tracker: MangaTracker,
    ) {
        if (tracker !is EnhancedMangaTracker) {
            return
        }

        val sortedChapters = getChaptersByMangaId.await(mangaId)
            .sortedBy { it.chapterNumber }
            .filter { it.isRecognizedNumber }

        val chapterUpdates = sortedChapters
            .filter { chapter -> chapter.chapterNumber <= remoteTrack.lastChapterRead && !chapter.read }
            .map { it.copy(read = true).toChapterUpdate() }

        // only take into account continuous reading
        val localLastRead = sortedChapters.takeWhile { it.read }.lastOrNull()?.chapterNumber ?: 0F
        val updatedTrack = remoteTrack.copy(lastChapterRead = localLastRead.toDouble())

        try {
            tracker.update(updatedTrack.toDbTrack())
            updateChapter.awaitAll(chapterUpdates)
            insertTrack.await(updatedTrack)
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e)
        }
    }
}
