package eu.kanade.domain.track.anime.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.domain.track.anime.interactor.GetAnimeTracks
import eu.kanade.domain.track.anime.interactor.InsertAnimeTrack
import eu.kanade.domain.track.anime.model.toDbTrack
import eu.kanade.domain.track.anime.store.DelayedAnimeTrackingStore
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class DelayedAnimeTrackingUpdateJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val getTracks = Injekt.get<GetAnimeTracks>()
        val insertTrack = Injekt.get<InsertAnimeTrack>()

        val trackManager = Injekt.get<TrackManager>()
        val delayedTrackingStore = Injekt.get<DelayedAnimeTrackingStore>()

        withIOContext {
            val tracks = delayedTrackingStore.getAnimeItems().mapNotNull {
                val track = getTracks.awaitOne(it.trackId)
                if (track == null) {
                    delayedTrackingStore.removeAnimeItem(it.trackId)
                }
                track
            }

            tracks.forEach { animeTrack ->
                try {
                    val service = trackManager.getService(animeTrack.syncId)
                    if (service != null && service.isLogged) {
                        service.animeService.update(animeTrack.toDbTrack(), true)
                        insertTrack.await(animeTrack)
                    }
                    delayedTrackingStore.removeAnimeItem(animeTrack.id)
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

            val request = OneTimeWorkRequestBuilder<DelayedAnimeTrackingUpdateJob>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
