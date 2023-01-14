package eu.kanade.tachiyomi.data.animelib

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import eu.kanade.data.episode.NoEpisodesException
import eu.kanade.domain.anime.interactor.GetAnime
import eu.kanade.domain.anime.interactor.GetAnimelibAnime
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.toAnimeUpdate
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.domain.animetrack.interactor.GetAnimeTracks
import eu.kanade.domain.animetrack.interactor.InsertAnimeTrack
import eu.kanade.domain.animetrack.model.toDbTrack
import eu.kanade.domain.animetrack.model.toDomainTrack
import eu.kanade.domain.category.interactor.GetAnimeCategories
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.download.service.DownloadPreferences
import eu.kanade.domain.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.episode.interactor.SyncEpisodesWithTrackServiceTwoWay
import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadManager
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadService
import eu.kanade.tachiyomi.data.animelib.AnimelibUpdateService.Companion.start
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.MANGA_HAS_UNREAD
import eu.kanade.tachiyomi.data.preference.MANGA_NON_COMPLETED
import eu.kanade.tachiyomi.data.preference.MANGA_NON_READ
import eu.kanade.tachiyomi.data.track.EnhancedTrackService
import eu.kanade.tachiyomi.data.track.MangaTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.prepUpdateCover
import eu.kanade.tachiyomi.util.shouldDownloadNewEpisodes
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.acquireWakeLock
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import eu.kanade.tachiyomi.util.system.getSerializableExtraCompat
import eu.kanade.tachiyomi.util.system.isServiceRunning
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class will take care of updating the episodes of the anime from the library. It can be
 * started calling the [start] method. If it's already running, it won't do anything.
 * While the library is updating, a [PowerManager.WakeLock] will be held until the update is
 * completed, preventing the device from going to sleep mode. A notification will display the
 * progress of the update, and if case of an unexpected error, this service will be silently
 * destroyed.
 */
class AnimelibUpdateService(
    val sourceManager: AnimeSourceManager = Injekt.get(),
    val downloadPreferences: DownloadPreferences = Injekt.get(),
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    val downloadManager: AnimeDownloadManager = Injekt.get(),
    val trackManager: TrackManager = Injekt.get(),
    val coverCache: AnimeCoverCache = Injekt.get(),
    private val getAnimelibAnime: GetAnimelibAnime = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId = Injekt.get(),
    private val getCategories: GetAnimeCategories = Injekt.get(),
    private val syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get(),
    private val getTracks: GetAnimeTracks = Injekt.get(),
    private val insertTrack: InsertAnimeTrack = Injekt.get(),
    private val syncEpisodesWithTrackServiceTwoWay: SyncEpisodesWithTrackServiceTwoWay = Injekt.get(),
) : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var notifier: AnimelibUpdateNotifier
    private var ioScope: CoroutineScope? = null

    private var animeToUpdate: List<AnimelibAnime> = mutableListOf()
    private var updateJob: Job? = null

    /**
     * Defines what should be updated within a service execution.
     */
    enum class Target {
        EPISODES, // Anime episodes
        COVERS, // Anime covers
        TRACKING, // Tracking metadata
    }

    companion object {

        private var instance: AnimelibUpdateService? = null

        /**
         * Key for category to update.
         */
        const val KEY_CATEGORY = "category"

        /**
         * Key that defines what should be updated.
         */
        const val KEY_TARGET = "target"

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(context: Context): Boolean {
            return context.isServiceRunning(AnimelibUpdateService::class.java)
        }

        /**
         * Starts the service. It will be started only if there isn't another instance already
         * running.
         *
         * @param context the application context.
         * @param category a specific category to update, or null for global update.
         * @param target defines what should be updated.
         * @return true if service newly started, false otherwise
         */
        fun start(context: Context, category: Category? = null, target: Target = Target.EPISODES): Boolean {
            if (isRunning(context)) return false
            val intent = Intent(context, AnimelibUpdateService::class.java).apply {
                putExtra(KEY_TARGET, target)
                category?.let { putExtra(KEY_CATEGORY, it.id) }
            }
            ContextCompat.startForegroundService(context, intent)

            return true
        }

        /**
         * Stops the service.
         *
         * @param context the application context.
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, AnimelibUpdateService::class.java))
        }
    }

    /**
     * Method called when the service is created. It injects dagger dependencies and acquire
     * the wake lock.
     */
    override fun onCreate() {
        super.onCreate()

        notifier = AnimelibUpdateNotifier(this)
        wakeLock = acquireWakeLock(javaClass.name)

        startForeground(Notifications.ID_LIBRARY_PROGRESS, notifier.progressNotificationBuilder.build())
    }

    /**
     * Method called when the service is destroyed. It destroys subscriptions and releases the wake
     * lock.
     */
    override fun onDestroy() {
        updateJob?.cancel()
        ioScope?.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }

    /**
     * This method needs to be implemented, but it's not used/needed.
     */
    override fun onBind(intent: Intent): IBinder? = null

    /**
     * Method called when the service receives an intent.
     *
     * @param intent the start intent from.
     * @param flags the flags of the command.
     * @param startId the start id of this command.
     * @return the start value of the command.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val target = intent.getSerializableExtraCompat<Target>(KEY_TARGET)
            ?: return START_NOT_STICKY

        instance = this

        // Unsubscribe from any previous subscription if needed
        updateJob?.cancel()
        ioScope?.cancel()

        // If this is a episode update; set the last update time to now
        if (target == Target.EPISODES) {
            libraryPreferences.libraryUpdateLastTimestamp().set(Date().time)
        }

        // Update favorite anime
        val categoryId = intent.getLongExtra(KEY_CATEGORY, -1L)
        addAnimeToQueue(categoryId)

        // Destroy service when completed or in case of an error.
        val handler = CoroutineExceptionHandler { _, exception ->
            logcat(LogPriority.ERROR, exception)
            stopSelf(startId)
        }
        ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        updateJob = ioScope?.launch(handler) {
            when (target) {
                Target.EPISODES -> updateEpisodeList()
                Target.COVERS -> updateCovers()
                Target.TRACKING -> updateTrackings()
            }
        }
        updateJob?.invokeOnCompletion { stopSelf(startId) }

        return START_REDELIVER_INTENT
    }

    private val isUpdateJobActive: Boolean
        get() = (updateJob?.isActive == true)

    /**
     * Adds list of anime to be updated.
     *
     * @param categoryId the ID of the category to update, or -1 if no category specified.
     */
    private fun addAnimeToQueue(categoryId: Long) {
        val animelibAnime = runBlocking { getAnimelibAnime.await() }

        val listToUpdate = if (categoryId != -1L) {
            animelibAnime.filter { it.category == categoryId }
        } else {
            val categoriesToUpdate = libraryPreferences.animelibUpdateCategories().get().map { it.toLong() }
            val includedAnime = if (categoriesToUpdate.isNotEmpty()) {
                animelibAnime.filter { it.category in categoriesToUpdate }
            } else {
                animelibAnime
            }

            val categoriesToExclude = libraryPreferences.animelibUpdateCategoriesExclude().get().map { it.toLong() }
            val excludedAnimeIds = if (categoriesToExclude.isNotEmpty()) {
                animelibAnime.filter { it.category in categoriesToExclude }.map { it.anime.id }
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
        // TODO: show warnings in stable
        if (maxUpdatesFromSource > ANIME_PER_SOURCE_QUEUE_WARNING_THRESHOLD) {
            notifier.showQueueSizeWarningNotification()
        }
    }

    /**
     * Method that updates the anime in [animeToUpdate]. It's called in a background thread, so it's safe
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
        val loggedServices by lazy { trackManager.services.filter { it.isLogged && it !is MangaTrackService } }
        val currentUnseenUpdatesCount = libraryPreferences.unseenUpdatesCount().get()
        val restrictions = libraryPreferences.libraryUpdateMangaRestriction().get()

        withIOContext {
            animeToUpdate.groupBy { it.anime.source }.values
                .map { animeInSource ->
                    async {
                        semaphore.withPermit {
                            animeInSource.forEach { animelibAnime ->
                                val anime = animelibAnime.anime
                                if (!isUpdateJobActive) {
                                    notifier.cancelProgressNotification()
                                    return@async
                                }

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
                                        MANGA_NON_COMPLETED in restrictions && anime.status.toInt() == SAnime.COMPLETED ->
                                            skippedUpdates.add(anime to getString(R.string.skipped_reason_completed))

                                        MANGA_HAS_UNREAD in restrictions && animelibAnime.unseenCount != 0L ->
                                            skippedUpdates.add(anime to getString(R.string.skipped_reason_not_caught_up))

                                        MANGA_NON_READ in restrictions && animelibAnime.totalEpisodes > 0L && !animelibAnime.hasStarted ->
                                            skippedUpdates.add(anime to getString(R.string.skipped_reason_not_started))

                                        anime.updateStrategy != UpdateStrategy.ALWAYS_UPDATE ->
                                            skippedUpdates.add(anime to getString(R.string.skipped_reason_not_always_update))

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

                                                    // Convert to the anime that contains new chapters
                                                    newUpdates.add(anime to newEpisodes.toTypedArray())
                                                }
                                            } catch (e: Throwable) {
                                                val errorMessage = when (e) {
                                                    is NoEpisodesException -> getString(R.string.no_episodes_error)
                                                    // failedUpdates will already have the source, don't need to copy it into the message
                                                    is AnimeSourceManager.AnimeSourceNotInstalledException -> getString(R.string.loader_not_implemented_error)
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
            val newEpisodeCount = newUpdates.sumOf { it.second.size }
            libraryPreferences.unseenUpdatesCount().set(currentUnseenUpdatesCount + newEpisodeCount)
            if (hasDownloads.get()) {
                AnimeDownloadService.start(this)
            }
        }

        if (failedUpdates.isNotEmpty()) {
            val errorFile = writeErrorFile(failedUpdates)
            notifier.showUpdateErrorNotification(
                failedUpdates.size,
                errorFile.getUriCompat(this),
            )
        }
        if (skippedUpdates.isNotEmpty()) {
            notifier.showUpdateSkippedNotification(skippedUpdates.size)
        }
    }

    private fun downloadEpisodes(anime: Anime, episodes: List<Episode>) {
        // We don't want to start downloading while the library is updating, because websites
        // may don't like it and they could ban the user.
        downloadManager.downloadEpisodes(anime, episodes.map { it.toDbEpisode() }, false)
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

        withIOContext {
            animeToUpdate.groupBy { it.anime.source }
                .values
                .map { animeInSource ->
                    async {
                        semaphore.withPermit {
                            animeInSource.forEach { animelibAnime ->
                                val anime = animelibAnime.anime
                                if (!isUpdateJobActive) {
                                    notifier.cancelProgressNotification()
                                    return@async
                                }

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
                                            logcat(LogPriority.ERROR) { "Manga doesn't exist anymore" }
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
        var progressCount = 0
        val loggedServices = trackManager.services.filter { it.isLogged && it !is MangaTrackService }

        animeToUpdate.forEach { animelibAnime ->
            val anime = animelibAnime.anime
            if (!isUpdateJobActive) {
                notifier.cancelProgressNotification()

                notifier.showProgressNotification(
                    listOf(anime),
                    progressCount++,
                    animeToUpdate.size,
                )

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
                                val updatedTrack = service.refresh(track.toDbTrack())
                                insertTrack.await(updatedTrack.toDomainTrack()!!)

                                if (service is EnhancedTrackService) {
                                    val episodes = getEpisodeByAnimeId.await(anime.id)
                                    syncEpisodesWithTrackServiceTwoWay.await(episodes, track, service)
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
        if (!isUpdateJobActive) {
            notifier.cancelProgressNotification()
            return
        }

        updatingAnime.add(anime)
        notifier.showProgressNotification(
            updatingAnime,
            completed.get(),
            animeToUpdate.size,
        )

        block()

        if (!isUpdateJobActive) {
            notifier.cancelProgressNotification()
            return
        }

        updatingAnime.remove(anime)
        completed.getAndIncrement()
        notifier.showProgressNotification(
            updatingAnime,
            completed.get(),
            animeToUpdate.size,
        )
    }

    /**
     * Writes basic file of update errors to cache dir.
     */
    private fun writeErrorFile(errors: List<Pair<Anime, String?>>): File {
        try {
            if (errors.isNotEmpty()) {
                val file = createFileInCacheDir("aniyomi_update_errors.txt")
                file.bufferedWriter().use { out ->
                    out.write(getString(R.string.library_errors_help, ERROR_LOG_HELP_URL) + "\n\n")
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
}

private const val ANIME_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 60
private const val ERROR_LOG_HELP_URL = "https://aniyomi.org/help/guides/troubleshooting"
