package eu.kanade.domain.track.anime.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import eu.kanade.domain.track.anime.model.toDbTrack
import eu.kanade.domain.track.anime.store.DelayedAnimeTrackingStore
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.system.workManager
import logcat.LogPriority
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import tachiyomi.domain.track.anime.interactor.InsertAnimeTrack
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

        val results = withIOContext {
            delayedTrackingStore.getAnimeItems()
                .mapNotNull {
                    val track = getTracks.awaitOne(it.trackId)
                    if (track == null) {
                        delayedTrackingStore.removeAnimeItem(it.trackId)
                    }
                    track?.copy(lastEpisodeSeen = it.lastEpisodeSeen.toDouble())
                }
                .mapNotNull { animeTrack ->
                    try {
                        val service = trackManager.getService(animeTrack.syncId)
                        if (service != null && service.isLogged) {
                            logcat(LogPriority.DEBUG) { "Updating delayed track item: ${animeTrack.id}, last episode seen: ${animeTrack.lastEpisodeSeen}" }
                            service.animeService.update(animeTrack.toDbTrack(), true)
                            insertTrack.await(animeTrack)
                        }
                        delayedTrackingStore.removeAnimeItem(animeTrack.id)
                        null
                    } catch (e: Exception) {
                        logcat(LogPriority.ERROR, e)
                        false
                    }
                }
        }

        return if (results.isNotEmpty()) Result.failure() else Result.success()
    }

    companion object {
        private const val TAG = "DelayedTrackingUpdate"

        fun setupTask(context: Context) {
            val constraints = Constraints(
                requiredNetworkType = NetworkType.CONNECTED,
            )

            val request = OneTimeWorkRequestBuilder<DelayedAnimeTrackingUpdateJob>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()

            context.workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
