package eu.kanade.tachiyomi.data.animelib

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.toSAnime
import eu.kanade.tachiyomi.animesource.model.toSEpisode
import eu.kanade.tachiyomi.data.animelib.AnimelibUpdateService.Companion.start
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimelibAnime
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.models.toAnimeInfo
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.AnimeDownloadService
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.MANGA_FULLY_READ
import eu.kanade.tachiyomi.data.preference.MANGA_ONGOING
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.EnhancedTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.util.episode.NoEpisodesException
import eu.kanade.tachiyomi.util.episode.syncEpisodesWithSource
import eu.kanade.tachiyomi.util.episode.syncEpisodesWithTrackServiceTwoWay
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.prepUpdateCover
import eu.kanade.tachiyomi.util.shouldDownloadNewEpisodes
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.acquireWakeLock
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import eu.kanade.tachiyomi.util.system.isServiceRunning
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class will take care of updating the chapters of the anime from the library. It can be
 * started calling the [start] method. If it's already running, it won't do anything.
 * While the library is updating, a [PowerManager.WakeLock] will be held until the update is
 * completed, preventing the device from going to sleep mode. A notification will display the
 * progress of the update, and if case of an unexpected error, this service will be silently
 * destroyed.
 */
class AnimelibUpdateService(
    val db: AnimeDatabaseHelper = Injekt.get(),
    val sourceManager: AnimeSourceManager = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    val downloadManager: AnimeDownloadManager = Injekt.get(),
    val trackManager: TrackManager = Injekt.get(),
    val coverCache: AnimeCoverCache = Injekt.get()
) : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var notifier: AnimelibUpdateNotifier
    private lateinit var ioScope: CoroutineScope

    private var animeToUpdate: List<AnimelibAnime> = mutableListOf()
    private var updateJob: Job? = null

    /**
     * Defines what should be updated within a service execution.
     */
    enum class Target {
        CHAPTERS, // Anime chapters

        COVERS, // Anime covers

        TRACKING // Tracking metadata
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
        fun start(context: Context, category: Category? = null, target: Target = Target.CHAPTERS): Boolean {
            return if (!isRunning(context)) {
                val intent = Intent(context, AnimelibUpdateService::class.java).apply {
                    putExtra(KEY_TARGET, target)
                    category?.let { putExtra(KEY_CATEGORY, it.id) }
                }
                ContextCompat.startForegroundService(context, intent)

                true
            } else {
                instance?.addAnimeToQueue(category?.id ?: -1, target)
                false
            }
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

        ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
        ioScope.cancel()
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
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

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
        val target = intent.getSerializableExtra(KEY_TARGET) as? Target
            ?: return START_NOT_STICKY

        instance = this

        // Unsubscribe from any previous subscription if needed
        updateJob?.cancel()

        // Update favorite anime
        val categoryId = intent.getIntExtra(KEY_CATEGORY, -1)
        addAnimeToQueue(categoryId, target)

        // Destroy service when completed or in case of an error.
        val handler = CoroutineExceptionHandler { _, exception ->
            logcat(LogPriority.ERROR, exception)
            stopSelf(startId)
        }
        updateJob = ioScope.launch(handler) {
            when (target) {
                Target.CHAPTERS -> updateEpisodeList()
                Target.COVERS -> updateCovers()
                Target.TRACKING -> updateTrackings()
            }
        }
        updateJob?.invokeOnCompletion { stopSelf(startId) }

        return START_REDELIVER_INTENT
    }

    /**
     * Adds list of anime to be updated.
     *
     * @param category the ID of the category to update, or -1 if no category specified.
     * @param target the target to update.
     */
    fun addAnimeToQueue(categoryId: Int, target: Target) {
        val animelibAnime = db.getAnimelibAnimes().executeAsBlocking()

        var listToUpdate = if (categoryId != -1) {
            animelibAnime.filter { it.category == categoryId }
        } else {
            val categoriesToUpdate = preferences.animelibUpdateCategories().get().map(String::toInt)
            val listToInclude = if (categoriesToUpdate.isNotEmpty()) {
                animelibAnime.filter { it.category in categoriesToUpdate }
            } else {
                animelibAnime
            }

            val categoriesToExclude = preferences.animelibUpdateCategoriesExclude().get().map(String::toInt)
            val listToExclude = if (categoriesToExclude.isNotEmpty()) {
                animelibAnime.filter { it.category in categoriesToExclude }
            } else {
                emptyList()
            }

            listToInclude.minus(listToExclude)
        }

        if (target == Target.CHAPTERS) {
            val restrictions = preferences.libraryUpdateMangaRestriction().get()
            if (MANGA_ONGOING in restrictions) {
                listToUpdate = listToUpdate.filterNot { it.status == SAnime.COMPLETED }
            }
            if (MANGA_FULLY_READ in restrictions) {
                listToUpdate = listToUpdate.filter { it.unseen == 0 }
            }
        }

        animeToUpdate = listToUpdate
            .distinctBy { it.id }
            .sortedBy { it.title }

        // Warn when excessively checking a single source
        val maxUpdatesFromSource = animeToUpdate
            .groupBy { it.source }
            .filterKeys { sourceManager.get(it) !is UnmeteredSource }
            .maxOfOrNull { it.value.size } ?: 0
        if (maxUpdatesFromSource > ANIME_PER_SOURCE_QUEUE_WARNING_THRESHOLD) {
            toast(R.string.notification_size_warning, Toast.LENGTH_LONG)
        }
    }

    /**
     * Method that updates the given list of anime. It's called in a background thread, so it's safe
     * to do heavy operations or network calls here.
     * For each anime it calls [updateAnime] and updates the notification showing the current
     * progress.
     *
     * @param animeToUpdate the list to update
     * @return an observable delivering the progress of each update.
     */
    suspend fun updateEpisodeList() {
        val semaphore = Semaphore(5)
        val progressCount = AtomicInteger(0)
        val currentlyUpdatingAnime = CopyOnWriteArrayList<AnimelibAnime>()
        val newUpdates = CopyOnWriteArrayList<Pair<AnimelibAnime, Array<Episode>>>()
        val failedUpdates = CopyOnWriteArrayList<Pair<Anime, String?>>()
        val hasDownloads = AtomicBoolean(false)
        val loggedServices by lazy { trackManager.services.filter { it.isLogged } }
        val currentUnseenUpdatesCount = preferences.unseenUpdatesCount().get()

        withIOContext {
            animeToUpdate.groupBy { it.source }
                .values
                .map { animeInSource ->
                    async {
                        semaphore.withPermit {
                            animeInSource.forEach { anime ->
                                if (updateJob?.isActive != true) {
                                    return@async
                                }

                                withUpdateNotification(
                                    currentlyUpdatingAnime,
                                    progressCount,
                                    anime,
                                ) { anime ->
                                    try {
                                        val (newEpisodes, _) = updateAnime(anime)

                                        if (newEpisodes.isNotEmpty()) {
                                            if (anime.shouldDownloadNewEpisodes(db, preferences)) {
                                                downloadEpisodes(anime, newEpisodes)
                                                hasDownloads.set(true)
                                            }

                                            // Convert to the manga that contains new chapters
                                            newUpdates.add(
                                                anime to newEpisodes.sortedByDescending { ep -> ep.source_order }
                                                    .toTypedArray()
                                            )
                                        }
                                    } catch (e: Throwable) {
                                        val errorMessage = when (e) {
                                            is NoEpisodesException -> {
                                                getString(R.string.no_episodes_error)
                                            }
                                            is AnimeSourceManager.SourceNotInstalledException -> {
                                                // failedUpdates will already have the source, don't need to copy it into the message
                                                getString(R.string.loader_not_implemented_error)
                                            }
                                            else -> {
                                                e.message
                                            }
                                        }
                                        failedUpdates.add(anime to errorMessage)
                                    }

                                    if (preferences.autoUpdateTrackers()) {
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
            preferences.unseenUpdatesCount().set(currentUnseenUpdatesCount + newEpisodeCount)
            if (hasDownloads.get()) {
                AnimeDownloadService.start(this)
            }
        }

        if (failedUpdates.isNotEmpty()) {
            val errorFile = writeErrorFile(failedUpdates)
            notifier.showUpdateErrorNotification(
                failedUpdates.map { it.first.title },
                errorFile.getUriCompat(this)
            )
        }
    }

    private fun downloadEpisodes(anime: Anime, episodes: List<Episode>) {
        // We don't want to start downloading while the library is updating, because websites
        // may don't like it and they could ban the user.
        downloadManager.downloadEpisodes(anime, episodes, false)
    }

    /**
     * Updates the chapters for the given anime and adds them to the database.
     *
     * @param anime the anime to update.
     * @return a pair of the inserted and removed chapters.
     */
    suspend fun updateAnime(anime: Anime): Pair<List<Episode>, List<Episode>> {
        val source = sourceManager.getOrStub(anime.source)

        // Update anime details metadata
        if (preferences.autoUpdateMetadata()) {
            val updatedAnime = source.getAnimeDetails(anime.toAnimeInfo())
            val sAnime = updatedAnime.toSAnime()
            // Avoid "losing" existing cover
            if (!sAnime.thumbnail_url.isNullOrEmpty()) {
                anime.prepUpdateCover(coverCache, sAnime, false)
            } else {
                sAnime.thumbnail_url = anime.thumbnail_url
            }

            anime.copyFrom(sAnime)
            db.insertAnime(anime).executeAsBlocking()
        }

        val chapters = source.getEpisodeList(anime.toAnimeInfo())
            .map { it.toSEpisode() }

        return syncEpisodesWithSource(db, chapters, anime, source)
    }

    private suspend fun updateCovers() {
        val semaphore = Semaphore(5)
        val progressCount = AtomicInteger(0)
        val currentlyUpdatingAnime = CopyOnWriteArrayList<AnimelibAnime>()

        withIOContext {
            animeToUpdate.groupBy { it.source }
                .values
                .map { animeInSource ->
                    async {
                        semaphore.withPermit {
                            animeInSource.forEach { anime ->
                                if (updateJob?.isActive != true) {
                                    return@async
                                }

                                withUpdateNotification(
                                    currentlyUpdatingAnime,
                                    progressCount,
                                    anime,
                                ) { anime ->
                                    sourceManager.get(anime.source)?.let { source ->
                                        try {
                                            val networkAnime =
                                                source.getAnimeDetails(anime.toAnimeInfo())
                                            val sAnime = networkAnime.toSAnime()
                                            anime.prepUpdateCover(coverCache, sAnime, true)
                                            sAnime.thumbnail_url?.let {
                                                anime.thumbnail_url = it
                                                db.insertAnime(anime).executeAsBlocking()
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
                }
                .awaitAll()
        }

        coverCache.clearMemoryCache()
        notifier.cancelProgressNotification()
    }

    /**
     * Method that updates the metadata of the connected tracking services. It's called in a
     * background thread, so it's safe to do heavy operations or network calls here.
     */
    private suspend fun updateTrackings() {
        var progressCount = 0
        val loggedServices = trackManager.services.filter { it.isLogged }

        animeToUpdate.forEach { anime ->
            if (updateJob?.isActive != true) {
                return
            }

            notifier.showProgressNotification(listOf(anime), progressCount++, animeToUpdate.size)

            // Update the tracking details.
            updateTrackings(anime, loggedServices)
        }

        notifier.cancelProgressNotification()
    }

    private suspend fun updateTrackings(anime: AnimelibAnime, loggedServices: List<TrackService>) {
        db.getTracks(anime).executeAsBlocking()
            .map { track ->
                supervisorScope {
                    async {
                        val service = trackManager.getService(track.sync_id)
                        if (service != null && service in loggedServices) {
                            try {
                                val updatedTrack = service.refresh(track)
                                db.insertTrack(updatedTrack).executeAsBlocking()

                                if (service is EnhancedTrackService) {
                                    syncEpisodesWithTrackServiceTwoWay(db, db.getEpisodes(anime).executeAsBlocking(), track, service)
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
        updatingAnime: CopyOnWriteArrayList<AnimelibAnime>,
        completed: AtomicInteger,
        anime: AnimelibAnime,
        block: suspend (AnimelibAnime) -> Unit,
    ) {
        if (updateJob?.isActive != true) {
            return
        }

        updatingAnime.add(anime)
        notifier.showProgressNotification(
            updatingAnime,
            completed.get(),
            animeToUpdate.size
        )

        block(anime)

        if (updateJob?.isActive != true) {
            return
        }

        updatingAnime.remove(anime)
        completed.andIncrement
        notifier.showProgressNotification(
            updatingAnime,
            completed.get(),
            animeToUpdate.size
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
                        out.write("! ${error}\n")
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
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }
}

private const val ANIME_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 60
private const val ERROR_LOG_HELP_URL = "https://aniyomi.jmir.xyz/help/guides/troubleshooting"
