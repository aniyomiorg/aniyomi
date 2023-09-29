package eu.kanade.tachiyomi.data.library.anime

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.domain.entries.anime.interactor.UpdateAnime
import eu.kanade.domain.entries.anime.model.copyFrom
import eu.kanade.domain.entries.anime.model.toSAnime
import eu.kanade.domain.items.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.items.episode.interactor.SyncEpisodesWithTrackServiceTwoWay
import eu.kanade.domain.track.anime.model.toDbTrack
import eu.kanade.domain.track.anime.model.toDomainTrack
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.UnmeteredSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.track.EnhancedAnimeTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.util.prepUpdateCover
import eu.kanade.tachiyomi.util.shouldDownloadNewEpisodes
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import tachiyomi.core.preference.getAndSet
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.interactor.GetLibraryAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.toAnimeUpdate
import tachiyomi.domain.items.episode.interactor.GetEpisodeByAnimeId
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.model.NoEpisodesException
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_BATTERY_NOT_LOW
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_HAS_UNVIEWED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_VIEWED
import tachiyomi.domain.source.anime.model.AnimeSourceNotInstalledException
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import tachiyomi.domain.track.anime.interactor.InsertAnimeTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class AnimeLibraryUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val sourceManager: AnimeSourceManager = Injekt.get()
    private val downloadPreferences: DownloadPreferences = Injekt.get()
    private val libraryPreferences: LibraryPreferences = Injekt.get()
    private val downloadManager: AnimeDownloadManager = Injekt.get()
    private val trackManager: TrackManager = Injekt.get()
    private val coverCache: AnimeCoverCache = Injekt.get()
    private val getLibraryAnime: GetLibraryAnime = Injekt.get()
    private val getAnime: GetAnime = Injekt.get()
    private val updateAnime: UpdateAnime = Injekt.get()
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId = Injekt.get()
    private val getCategories: GetAnimeCategories = Injekt.get()
    private val syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get()
    private val getTracks: GetAnimeTracks = Injekt.get()
    private val insertTrack: InsertAnimeTrack = Injekt.get()
    private val syncEpisodesWithTrackServiceTwoWay: SyncEpisodesWithTrackServiceTwoWay = Injekt.get()

    private val notifier = AnimeLibraryUpdateNotifier(context)

    private var animeToUpdate: List<LibraryAnime> = mutableListOf()

    override suspend fun doWork(): Result {
        if (tags.contains(WORK_NAME_AUTO)) {
            val preferences = Injekt.get<LibraryPreferences>()
            val restrictions = preferences.libraryUpdateDeviceRestriction().get()
            if ((DEVICE_ONLY_ON_WIFI in restrictions) && !context.isConnectedToWifi()) {
                return Result.failure()
            }

            // Find a running manual worker. If exists, try again later
            if (context.workManager.isRunning(WORK_NAME_MANUAL)) {
                return Result.retry()
            }
        }

        try {
            setForeground(getForegroundInfo())
        } catch (e: IllegalStateException) {
            logcat(LogPriority.ERROR, e) { "Not allowed to set foreground job" }
        }

        val target = inputData.getString(KEY_TARGET)?.let { Target.valueOf(it) } ?: Target.EPISODES

        // If this is a chapter update, set the last update time to now
        if (target == Target.EPISODES) {
            libraryPreferences.libraryUpdateLastTimestamp().set(Date().time)
        }

        val categoryId = inputData.getLong(KEY_CATEGORY, -1L)
        addAnimeToQueue(categoryId)

        return withIOContext {
            try {
                when (target) {
                    Target.EPISODES -> updateEpisodeList()
                    Target.COVERS -> updateCovers()
                    Target.TRACKING -> updateTrackings()
                }
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
        )
    }

    /**
     * Adds list of anime to be updated.
     *
     * @param categoryId the ID of the category to update, or -1 if no category specified.
     */
    private fun addAnimeToQueue(categoryId: Long) {
        val libraryAnime = runBlocking { getLibraryAnime.await() }

        val listToUpdate = if (categoryId != -1L) {
            libraryAnime.filter { it.category == categoryId }
        } else {
            val categoriesToUpdate = libraryPreferences.animeLibraryUpdateCategories().get().map { it.toLong() }
            val includedAnime = if (categoriesToUpdate.isNotEmpty()) {
                libraryAnime.filter { it.category in categoriesToUpdate }
            } else {
                libraryAnime
            }

            val categoriesToExclude = libraryPreferences.animeLibraryUpdateCategoriesExclude().get().map { it.toLong() }
            val excludedAnimeIds = if (categoriesToExclude.isNotEmpty()) {
                libraryAnime.filter { it.category in categoriesToExclude }.map { it.anime.id }
            } else {
                emptyList()
            }

            includedAnime
                .filterNot { it.anime.id in excludedAnimeIds }
                .distinctBy { it.anime.id }
        }

        animeToUpdate = listToUpdate
            .sortedBy { it.anime.title }

        // Warn when excessively checking a single source
        val maxUpdatesFromSource = animeToUpdate
            .groupBy { it.anime.source }
            .filterKeys { sourceManager.get(it) !is UnmeteredSource }
            .maxOfOrNull { it.value.size } ?: 0
        if (maxUpdatesFromSource > ANIME_PER_SOURCE_QUEUE_WARNING_THRESHOLD) {
            notifier.showQueueSizeWarningNotification()
        }
    }

    /**
     * Method that updates anime in [animeToUpdate]. It's called in a background thread, so it's safe
     * to do heavy operations or network calls here.
     * For each anime it calls [updateAnime] and updates the notification showing the current
     * progress.
     *
     * @return an observable delivering the progress of each update.
     */
    private suspend fun updateEpisodeList() {
        val semaphore = Semaphore(5)
        val progressCount = AtomicInteger(0)
        val currentlyUpdatingAnime = CopyOnWriteArrayList<Anime>()
        val newUpdates = CopyOnWriteArrayList<Pair<Anime, Array<Episode>>>()
        val skippedUpdates = CopyOnWriteArrayList<Pair<Anime, String?>>()
        val failedUpdates = CopyOnWriteArrayList<Pair<Anime, String?>>()
        val hasDownloads = AtomicBoolean(false)
        val loggedServices by lazy { trackManager.services.filter { it.isLogged } }
        val restrictions = libraryPreferences.libraryUpdateItemRestriction().get()

        coroutineScope {
            animeToUpdate.groupBy { it.anime.source }.values
                .map { animeInSource ->
                    async {
                        semaphore.withPermit {
                            animeInSource.forEach { libraryAnime ->
                                val anime = libraryAnime.anime
                                ensureActive()

                                // Don't continue to update if anime is not in library
                                if (getAnime.await(anime.id)?.favorite != true) {
                                    return@forEach
                                }

                                withUpdateNotification(
                                    currentlyUpdatingAnime,
                                    progressCount,
                                    anime,
                                ) {
                                    when {
                                        ENTRY_NON_COMPLETED in restrictions && anime.status.toInt() == SAnime.COMPLETED ->
                                            skippedUpdates.add(anime to context.getString(R.string.skipped_reason_completed))

                                        ENTRY_HAS_UNVIEWED in restrictions && libraryAnime.unseenCount != 0L ->
                                            skippedUpdates.add(anime to context.getString(R.string.skipped_reason_not_caught_up))

                                        ENTRY_NON_VIEWED in restrictions && libraryAnime.totalEpisodes > 0L && !libraryAnime.hasStarted ->
                                            skippedUpdates.add(anime to context.getString(R.string.skipped_reason_not_started))

                                        anime.updateStrategy != UpdateStrategy.ALWAYS_UPDATE ->
                                            skippedUpdates.add(anime to context.getString(R.string.skipped_reason_not_always_update))

                                        else -> {
                                            try {
                                                val newEpisodes = updateAnime(anime)
                                                    .sortedByDescending { it.sourceOrder }

                                                if (newEpisodes.isNotEmpty()) {
                                                    val categoryIds = getCategories.await(anime.id).map { it.id }
                                                    if (anime.shouldDownloadNewEpisodes(categoryIds, downloadPreferences)) {
                                                        downloadEpisodes(anime, newEpisodes)
                                                        hasDownloads.set(true)
                                                    }

                                                    libraryPreferences.newAnimeUpdatesCount().getAndSet { it + newEpisodes.size }

                                                    // Convert to the anime that contains new chapters
                                                    newUpdates.add(anime to newEpisodes.toTypedArray())
                                                }
                                            } catch (e: Throwable) {
                                                val errorMessage = when (e) {
                                                    is NoEpisodesException -> context.getString(R.string.no_episodes_error)
                                                    // failedUpdates will already have the source, don't need to copy it into the message
                                                    is AnimeSourceNotInstalledException -> context.getString(R.string.loader_not_implemented_error)
                                                    else -> e.message
                                                }
                                                failedUpdates.add(anime to errorMessage)
                                            }
                                        }
                                    }

                                    if (libraryPreferences.autoUpdateTrackers().get()) {
                                        updateTrackings(anime, loggedServices)
                                    }
                                }
                            }
                        }
                    }
                }
                .awaitAll()
        }

        notifier.cancelProgressNotification()

        if (newUpdates.isNotEmpty()) {
            notifier.showUpdateNotifications(newUpdates)
            if (hasDownloads.get()) {
                downloadManager.startDownloads()
            }
        }

        if (failedUpdates.isNotEmpty()) {
            val errorFile = writeErrorFile(failedUpdates)
            notifier.showUpdateErrorNotification(
                failedUpdates.size,
                errorFile.getUriCompat(context),
            )
        }
        if (skippedUpdates.isNotEmpty()) {
            notifier.showUpdateSkippedNotification(skippedUpdates.size)
        }
    }

    private fun downloadEpisodes(anime: Anime, episodes: List<Episode>) {
        // We don't want to start downloading while the library is updating, because websites
        // may don't like it and they could ban the user.
        downloadManager.downloadEpisodes(anime, episodes, false)
    }

    /**
     * Updates the episodes for the given anime and adds them to the database.
     *
     * @param anime the anime to update.
     * @return a pair of the inserted and removed episodes.
     */
    private suspend fun updateAnime(anime: Anime): List<Episode> {
        val source = sourceManager.getOrStub(anime.source)

        // Update anime metadata if needed
        if (libraryPreferences.autoUpdateMetadata().get()) {
            val networkAnime = source.getAnimeDetails(anime.toSAnime())
            updateAnime.awaitUpdateFromSource(anime, networkAnime, manualFetch = false, coverCache)
        }

        val episodes = source.getEpisodeList(anime.toSAnime())

        // Get anime from database to account for if it was removed during the update and
        // to get latest data so it doesn't get overwritten later on
        val dbAnime = getAnime.await(anime.id)?.takeIf { it.favorite } ?: return emptyList()

        return syncEpisodesWithSource.await(episodes, dbAnime, source)
    }

    private suspend fun updateCovers() {
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
                                        val updatedAnime = anime.prepUpdateCover(coverCache, networkAnime, true)
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

    /**
     * Method that updates the metadata of the connected tracking services. It's called in a
     * background thread, so it's safe to do heavy operations or network calls here.
     */
    private suspend fun updateTrackings() {
        coroutineScope {
            var progressCount = 0
            val loggedServices = trackManager.services.filter { it.isLogged }

            animeToUpdate.forEach { libraryAnime ->
                val anime = libraryAnime.anime

                ensureActive()

                notifier.showProgressNotification(listOf(anime), progressCount++, animeToUpdate.size)

                // Update the tracking details.
                updateTrackings(anime, loggedServices)
            }

            notifier.cancelProgressNotification()
        }
    }

    private suspend fun updateTrackings(anime: Anime, loggedServices: List<TrackService>) {
        getTracks.await(anime.id)
            .map { track ->
                supervisorScope {
                    async {
                        val service = trackManager.getService(track.syncId)
                        if (service != null && service in loggedServices) {
                            try {
                                val updatedTrack = service.animeService.refresh(track.toDbTrack())
                                insertTrack.await(updatedTrack.toDomainTrack()!!)

                                if (service is EnhancedAnimeTrackService) {
                                    val episodes = getEpisodeByAnimeId.await(anime.id)
                                    syncEpisodesWithTrackServiceTwoWay.await(episodes, track, service.animeService)
                                }
                            } catch (e: Throwable) {
                                // Ignore errors and continue
                                logcat(LogPriority.ERROR, e)
                            }
                        }
                    }
                }
            }
            .awaitAll()
    }

    private suspend fun withUpdateNotification(
        updatingAnime: CopyOnWriteArrayList<Anime>,
        completed: AtomicInteger,
        anime: Anime,
        block: suspend () -> Unit,
    ) {
        coroutineScope {
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
    }

    /**
     * Writes basic file of update errors to cache dir.
     */
    private fun writeErrorFile(errors: List<Pair<Anime, String?>>): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("aniyomi_update_errors.txt")
                file.bufferedWriter().use { out ->
                    out.write(context.getString(R.string.library_errors_help, ERROR_LOG_HELP_URL) + "\n\n")
                    // Error file format:
                    // ! Error
                    //   # Source
                    //     - Anime
                    errors.groupBy({ it.second }, { it.first }).forEach { (error, animes) ->
                        out.write("\n! ${error}\n")
                        animes.groupBy { it.source }.forEach { (srcId, animes) ->
                            val source = sourceManager.getOrStub(srcId)
                            out.write("  # $source\n")
                            animes.forEach {
                                out.write("    - ${it.title}\n")
                            }
                        }
                    }
                }
                return file
            }
        } catch (_: Exception) {}
        return File("")
    }

    /**
     * Defines what should be updated within a service execution.
     */
    enum class Target {
        EPISODES, // Anime episodes
        COVERS, // Anime covers
        TRACKING, // Tracking metadata
    }

    companion object {
        private const val TAG = "AnimeLibraryUpdate"
        private const val WORK_NAME_AUTO = "AnimeLibraryUpdate-auto"
        private const val WORK_NAME_MANUAL = "AnimeLibraryUpdate-manual"

        private const val ERROR_LOG_HELP_URL = "https://aniyomi.org/help/guides/troubleshooting"

        private const val ANIME_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 60

        /**
         * Key for category to update.
         */
        private const val KEY_CATEGORY = "animeCategory"

        /**
         * Key that defines what should be updated.
         */
        private const val KEY_TARGET = "animeTarget"

        fun cancelAllWorks(context: Context) {
            context.workManager.cancelAllWorkByTag(TAG)
        }

        fun setupTask(
            context: Context,
            prefInterval: Int? = null,
        ) {
            val preferences = Injekt.get<LibraryPreferences>()
            val interval = prefInterval ?: preferences.libraryUpdateInterval().get()
            if (interval > 0) {
                val restrictions = preferences.libraryUpdateDeviceRestriction().get()
                val constraints = Constraints(
                    requiredNetworkType = if (DEVICE_NETWORK_NOT_METERED in restrictions) { NetworkType.UNMETERED } else { NetworkType.CONNECTED },
                    requiresCharging = DEVICE_CHARGING in restrictions,
                    requiresBatteryNotLow = DEVICE_BATTERY_NOT_LOW in restrictions,
                )

                val request = PeriodicWorkRequestBuilder<AnimeLibraryUpdateJob>(
                    interval.toLong(),
                    TimeUnit.HOURS,
                    10,
                    TimeUnit.MINUTES,
                )
                    .addTag(TAG)
                    .addTag(WORK_NAME_AUTO)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                    .build()

                context.workManager.enqueueUniquePeriodicWork(WORK_NAME_AUTO, ExistingPeriodicWorkPolicy.UPDATE, request)
            } else {
                context.workManager.cancelUniqueWork(WORK_NAME_AUTO)
            }
        }
        fun startNow(
            context: Context,
            category: Category? = null,
            target: Target = Target.EPISODES,
        ): Boolean {
            val wm = context.workManager
            if (wm.isRunning(TAG)) {
                // Already running either as a scheduled or manual job
                return false
            }

            val inputData = workDataOf(
                KEY_CATEGORY to category?.id,
                KEY_TARGET to target.name,
            )
            val request = OneTimeWorkRequestBuilder<AnimeLibraryUpdateJob>()
                .addTag(TAG)
                .addTag(WORK_NAME_MANUAL)
                .setInputData(inputData)
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

                    // Re-enqueue cancelled scheduled work
                    if (it.tags.contains(WORK_NAME_AUTO)) {
                        setupTask(context)
                    }
                }
        }
    }
}
