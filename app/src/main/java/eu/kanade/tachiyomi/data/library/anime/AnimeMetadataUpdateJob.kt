package eu.kanade.tachiyomi.data.library.anime

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import eu.kanade.domain.entries.anime.interactor.UpdateAnime
import eu.kanade.domain.entries.anime.model.copyFrom
import eu.kanade.domain.entries.anime.model.toSAnime
import eu.kanade.tachiyomi.data.cache.AnimeBackgroundCache
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.prepUpdateBackground
import eu.kanade.tachiyomi.util.prepUpdateCover
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.anime.interactor.GetLibraryAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.toAnimeUpdate
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class AnimeMetadataUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val sourceManager: AnimeSourceManager = Injekt.get()
    private val coverCache: AnimeCoverCache = Injekt.get()
    private val backgroundCache: AnimeBackgroundCache = Injekt.get()
    private val getLibraryAnime: GetLibraryAnime = Injekt.get()
    private val updateAnime: UpdateAnime = Injekt.get()

    private val notifier = AnimeLibraryUpdateNotifier(context)

    private var animeToUpdate: List<LibraryAnime> = mutableListOf()

    override suspend fun doWork(): Result {
        try {
            setForeground(getForegroundInfo())
        } catch (e: IllegalStateException) {
            logcat(LogPriority.ERROR, e) { "Not allowed to set foreground job" }
        }

        addAnimeToQueue()

        return withIOContext {
            try {
                updateMetadata()
                Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Assume success although cancelled
                    Result.success()
                } else {
                    logcat(LogPriority.ERROR, e)
                    Result.failure()
                }
            } finally {
                notifier.cancelProgressNotification()
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notifier = AnimeLibraryUpdateNotifier(context)
        return ForegroundInfo(
            Notifications.ID_LIBRARY_PROGRESS,
            notifier.progressNotificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },

        )
    }

    /**
     * Adds list of anime to be updated.
     */
    private suspend fun addAnimeToQueue() {
        animeToUpdate = getLibraryAnime.await()
        notifier.showQueueSizeWarningNotificationIfNeeded(animeToUpdate)
    }

    private suspend fun updateMetadata() {
        val semaphore = Semaphore(5)
        val progressCount = AtomicInteger(0)
        val currentlyUpdatingAnime = CopyOnWriteArrayList<Anime>()

        coroutineScope {
            animeToUpdate.groupBy { it.anime.source }
                .values
                .map { animeInSource ->
                    async {
                        semaphore.withPermit {
                            animeInSource.forEach { libraryAnime ->
                                val anime = libraryAnime.anime
                                ensureActive()

                                withUpdateNotification(
                                    currentlyUpdatingAnime,
                                    progressCount,
                                    anime,
                                ) {
                                    val source = sourceManager.get(anime.source) ?: return@withUpdateNotification
                                    try {
                                        val networkAnime = source.getAnimeDetails(anime.toSAnime())
                                        val updatedAnime = anime
                                            .prepUpdateCover(coverCache, networkAnime, true)
                                            .prepUpdateBackground(backgroundCache, networkAnime, true)
                                            .copyFrom(networkAnime)
                                        try {
                                            updateAnime.await(updatedAnime.toAnimeUpdate())
                                        } catch (e: Exception) {
                                            logcat(LogPriority.ERROR) { "Anime doesn't exist anymore" }
                                        }
                                    } catch (e: Throwable) {
                                        // Ignore errors and continue
                                        logcat(LogPriority.ERROR, e)
                                    }
                                }
                            }
                        }
                    }
                }
                .awaitAll()
        }

        notifier.cancelProgressNotification()
    }

    private suspend fun withUpdateNotification(
        updatingAnime: CopyOnWriteArrayList<Anime>,
        completed: AtomicInteger,
        anime: Anime,
        block: suspend () -> Unit,
    ) = coroutineScope {
        ensureActive()

        updatingAnime.add(anime)
        notifier.showProgressNotification(
            updatingAnime,
            completed.get(),
            animeToUpdate.size,
        )

        block()

        ensureActive()

        updatingAnime.remove(anime)
        completed.getAndIncrement()
        notifier.showProgressNotification(
            updatingAnime,
            completed.get(),
            animeToUpdate.size,
        )
    }

    companion object {
        private const val TAG = "MetadataUpdate"
        private const val WORK_NAME_MANUAL = "MetadataUpdate"

        private const val MANGA_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 60

        fun startNow(context: Context): Boolean {
            val wm = context.workManager
            if (wm.isRunning(TAG)) {
                // Already running either as a scheduled or manual job
                return false
            }
            val request = OneTimeWorkRequestBuilder<AnimeMetadataUpdateJob>()
                .addTag(TAG)
                .addTag(WORK_NAME_MANUAL)
                .build()
            wm.enqueueUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, request)

            return true
        }

        fun stop(context: Context) {
            val wm = context.workManager
            val workQuery = WorkQuery.Builder.fromTags(listOf(TAG))
                .addStates(listOf(WorkInfo.State.RUNNING))
                .build()
            wm.getWorkInfos(workQuery).get()
                // Should only return one work but just in case
                .forEach {
                    wm.cancelWorkById(it.id)
                }
        }
    }
}
