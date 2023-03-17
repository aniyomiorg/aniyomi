package eu.kanade.domain.track.manga.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.domain.track.manga.interactor.GetMangaTracks
import eu.kanade.domain.track.manga.interactor.InsertMangaTrack
import eu.kanade.domain.track.manga.model.toDbTrack
import eu.kanade.domain.track.manga.store.DelayedMangaTrackingStore
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class DelayedMangaTrackingUpdateJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val getTracks = Injekt.get<GetMangaTracks>()
        val insertTrack = Injekt.get<InsertMangaTrack>()

        val trackManager = Injekt.get<TrackManager>()
        val delayedTrackingStore = Injekt.get<DelayedMangaTrackingStore>()

        withIOContext {
            val tracks = delayedTrackingStore.getMangaItems().mapNotNull {
                val track = getTracks.awaitOne(it.trackId)
                if (track == null) {
                    delayedTrackingStore.removeMangaItem(it.trackId)
                }
                track
            }

            tracks.forEach { track ->
                try {
                    val service = trackManager.getService(track.syncId)
                    if (service != null && service.isLogged) {
                        service.mangaService.update(track.toDbTrack(), true)
                        insertTrack.await(track)
                    }
                    delayedTrackingStore.removeMangaItem(track.id)
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e)
                }
            }
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "DelayedTrackingUpdate"

        fun setupTask(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DelayedMangaTrackingUpdateJob>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
