package eu.kanade.domain.items.chapter.interactor

import eu.kanade.domain.track.manga.model.toDbTrack
import eu.kanade.tachiyomi.data.track.MangaTrackService
import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.items.chapter.interactor.UpdateChapter
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.chapter.model.toChapterUpdate
import tachiyomi.domain.track.manga.interactor.InsertMangaTrack
import tachiyomi.domain.track.manga.model.MangaTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SyncChaptersWithTrackServiceTwoWay(
    private val updateChapter: UpdateChapter = Injekt.get(),
    private val insertTrack: InsertMangaTrack = Injekt.get(),
) {

    suspend fun await(
        chapters: List<Chapter>,
        remoteTrack: MangaTrack,
        service: MangaTrackService,
    ) {
        val sortedChapters = chapters.sortedBy { it.chapterNumber }
        val chapterUpdates = sortedChapters
            .filter { chapter -> chapter.chapterNumber <= remoteTrack.lastChapterRead && !chapter.read }
            .map { it.copy(read = true).toChapterUpdate() }

        // only take into account continuous reading
        val localLastRead = sortedChapters.takeWhile { it.read }.lastOrNull()?.chapterNumber ?: 0F
        val updatedTrack = remoteTrack.copy(lastChapterRead = localLastRead.toDouble())

        try {
            service.update(updatedTrack.toDbTrack())
            updateChapter.awaitAll(chapterUpdates)
            insertTrack.await(updatedTrack)
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e)
        }
    }
}
