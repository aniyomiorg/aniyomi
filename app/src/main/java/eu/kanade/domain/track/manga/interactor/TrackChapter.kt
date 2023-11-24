package eu.kanade.domain.track.manga.interactor

import android.content.Context
import eu.kanade.domain.track.manga.model.toDbTrack
import eu.kanade.domain.track.manga.service.DelayedMangaTrackingUpdateJob
import eu.kanade.domain.track.manga.store.DelayedMangaTrackingStore
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import logcat.LogPriority
import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.track.manga.interactor.GetMangaTracks
import tachiyomi.domain.track.manga.interactor.InsertMangaTrack

class TrackChapter(
    private val getTracks: GetMangaTracks,
    private val trackerManager: TrackerManager,
    private val insertTrack: InsertMangaTrack,
    private val delayedTrackingStore: DelayedMangaTrackingStore,
) {

    suspend fun await(context: Context, mangaId: Long, chapterNumber: Double) {
        withNonCancellableContext {
            val tracks = getTracks.await(mangaId)

            if (tracks.isEmpty()) return@withNonCancellableContext

            tracks.mapNotNull { track ->
                val tracker = trackerManager.get(track.syncId)
                if (tracker != null && tracker.isLoggedIn && chapterNumber > track.lastChapterRead) {
                    val updatedTrack = track.copy(lastChapterRead = chapterNumber)

                    async {
                        runCatching {
                            try {
                                tracker.mangaService.update(updatedTrack.toDbTrack(), true)
                                insertTrack.await(updatedTrack)
                            } catch (e: Exception) {
                                delayedTrackingStore.addMangaItem(updatedTrack)
                                DelayedMangaTrackingUpdateJob.setupTask(context)
                                throw e
                            }
                        }
                    }
                } else {
                    null
                }
            }
                .awaitAll()
                .mapNotNull { it.exceptionOrNull() }
                .forEach { logcat(LogPriority.INFO, it) }
        }
    }
}
