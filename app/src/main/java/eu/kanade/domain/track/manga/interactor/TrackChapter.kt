package eu.kanade.domain.track.manga.interactor

import android.content.Context
import eu.kanade.domain.track.manga.model.toDbTrack
import eu.kanade.domain.track.manga.service.DelayedMangaTrackingUpdateJob
import eu.kanade.domain.track.manga.store.DelayedMangaTrackingStore
import eu.kanade.tachiyomi.data.track.TrackManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import logcat.LogPriority
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.track.manga.interactor.GetMangaTracks
import tachiyomi.domain.track.manga.interactor.InsertMangaTrack

class TrackChapter(
    private val getTracks: GetMangaTracks,
    private val trackManager: TrackManager,
    private val insertTrack: InsertMangaTrack,
    private val delayedTrackingStore: DelayedMangaTrackingStore,
) {

    suspend fun await(context: Context, mangaId: Long, chapterNumber: Double) = coroutineScope {
        launchNonCancellable {
            val tracks = getTracks.await(mangaId)

            if (tracks.isEmpty()) return@launchNonCancellable

            tracks.mapNotNull { track ->
                val service = trackManager.getService(track.syncId)
                if (service != null && service.isLoggedIn && chapterNumber > track.lastChapterRead) {
                    val updatedTrack = track.copy(lastChapterRead = chapterNumber)

                    async {
                        runCatching {
                            try {
                                service.mangaService.update(updatedTrack.toDbTrack(), true)
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
