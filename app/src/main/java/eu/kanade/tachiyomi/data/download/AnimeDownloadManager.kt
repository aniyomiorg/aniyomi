package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.download.model.AnimeDownloadQueue
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * This class is used to manage episode downloads in the application. It must be instantiated once
 * and retrieved through dependency injection. You can use this class to queue new episodes or query
 * downloaded episodes.
 *
 * @param context the application context.
 */
class AnimeDownloadManager(
    private val context: Context,
    private val db: AnimeDatabaseHelper = Injekt.get(),
) {

    private val sourceManager: AnimeSourceManager by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * Downloads provider, used to retrieve the folders where the episodes are or should be stored.
     */
    private val provider = AnimeDownloadProvider(context)

    /**
     * Cache of downloaded episodes.
     */
    private val cache = AnimeDownloadCache(context, provider, sourceManager)

    /**
     * Downloader whose only task is to download episodes.
     */
    private val downloader = AnimeDownloader(context, provider, cache, sourceManager)

    /**
     * Queue to delay the deletion of a list of episodes until triggered.
     */
    private val pendingDeleter = AnimeDownloadPendingDeleter(context)

    /**
     * Downloads queue, where the pending episodes are stored.
     */
    val queue: AnimeDownloadQueue
        get() = downloader.queue

    /**
     * Subject for subscribing to downloader status.
     */
    val runningRelay: BehaviorRelay<Boolean>
        get() = downloader.runningRelay

    /**
     * Tells the downloader to begin downloads.
     *
     * @return true if it's started, false otherwise (empty queue).
     */
    fun startDownloads(): Boolean {
        return downloader.start()
    }

    /**
     * Tells the downloader to stop downloads.
     *
     * @param reason an optional reason for being stopped, used to notify the user.
     */
    fun stopDownloads(reason: String? = null) {
        downloader.stop(reason)
    }

    /**
     * Tells the downloader to pause downloads.
     */
    fun pauseDownloads() {
        downloader.pause()
    }

    /**
     * Empties the download queue.
     *
     * @param isNotification value that determines if status is set (needed for view updates)
     */
    fun clearQueue(isNotification: Boolean = false) {
        downloader.clearQueue(isNotification)
    }

    fun startDownloadNow(episode: Episode) {
        val download = downloader.queue.find { it.episode.id == episode.id } ?: return
        val queue = downloader.queue.toMutableList()
        queue.remove(download)
        queue.add(0, download)
        reorderQueue(queue)
        if (isPaused()) {
            if (AnimeDownloadService.isRunning(context)) {
                downloader.start()
            } else {
                AnimeDownloadService.start(context)
            }
        }
    }

    fun isPaused() = downloader.isPaused()

    /**
     * Reorders the download queue.
     *
     * @param downloads value to set the download queue to
     */
    fun reorderQueue(downloads: List<AnimeDownload>) {
        if (downloader.queue.queue == downloads) return
        val wasRunning = downloader.isRunning

        if (downloads.isEmpty()) {
            AnimeDownloadService.stop(context)
            downloader.queue.clear()
            return
        }

        downloader.pause()
        downloader.queue.clear()
        downloader.queue.addAll(downloads)

        if (wasRunning) {
            downloader.start()
        }
    }

    /**
     * Tells the downloader to enqueue the given list of episodes.
     *
     * @param anime the anime of the episodes.
     * @param episodes the list of episodes to enqueue.
     * @param autoStart whether to start the downloader after enqueing the episodes.
     */
    fun downloadEpisodes(anime: Anime, episodes: List<Episode>, autoStart: Boolean = true) {
        downloader.queueEpisodes(anime, episodes, autoStart)
    }

    /**
     * Tells the downloader to enqueue the given list of episodes.
     *
     * @param anime the anime of the episodes.
     * @param episodes the list of episodes to enqueue.
     * @param autoStart whether to start the downloader after enqueing the episodes.
     */
    fun downloadEpisodesExternally(anime: Anime, episodes: List<Episode>, autoStart: Boolean = true) {
        downloader.queueEpisodes(anime, episodes, autoStart, true)
    }

    /**
     * Builds the page list of a downloaded episode.
     *
     * @param source the source of the episode.
     * @param anime the anime of the episode.
     * @param episode the downloaded episode.
     * @return an observable containing the list of pages from the episode.
     */
    fun buildVideo(source: AnimeSource, anime: Anime, episode: Episode): Observable<Video> {
        return buildVideo(provider.findEpisodeDir(episode, anime, source))
    }

    /**
     * Builds the page list of a downloaded episode.
     *
     * @param episodeDir the file where the episode is downloaded.
     * @return an observable containing the list of pages from the episode.
     */
    private fun buildVideo(episodeDir: UniFile?): Observable<Video> {
        return Observable.fromCallable {
            val files = episodeDir?.listFiles().orEmpty()
                .filter { "video" in it.type.orEmpty() }

            if (files.isEmpty()) {
                throw Exception(context.getString(R.string.video_list_empty_error))
            }

            val file = files[0]
            Video(file.uri.toString(), "download: " + file.uri.toString(), file.uri.toString(), file.uri).apply { status = Video.READY }
        }
    }

    /**
     * Returns true if the episode is downloaded.
     *
     * @param episode the episode to check.
     * @param anime the anime of the episode.
     * @param skipCache whether to skip the directory cache and check in the filesystem.
     */
    fun isEpisodeDownloaded(episode: Episode, anime: Anime, skipCache: Boolean = false): Boolean {
        return cache.isEpisodeDownloaded(episode, anime, skipCache)
    }

    /**
     * Returns the download from queue if the episode is queued for download
     * else it will return null which means that the episode is not queued for download
     *
     * @param episode the episode to check.
     */
    fun getEpisodeDownloadOrNull(episode: Episode): AnimeDownload? {
        return downloader.queue
            .firstOrNull { it.episode.id == episode.id && it.episode.anime_id == episode.anime_id }
    }

    /**
     * Returns the amount of downloaded episodes for a anime.
     *
     * @param anime the anime to check.
     */
    fun getDownloadCount(anime: Anime): Int {
        return cache.getDownloadCount(anime)
    }

    /**
     * Calls delete episode, which deletes a temp download.
     *
     * @param download the download to cancel.
     */
    fun deletePendingDownload(download: AnimeDownload) {
        deleteEpisodes(listOf(download.episode), download.anime, download.source, true)
    }

    fun deletePendingDownloads(vararg downloads: AnimeDownload) {
        val downloadsByAnime = downloads.groupBy { it.anime.id }
        downloadsByAnime.map { entry ->
            val anime = entry.value.first().anime
            val source = entry.value.first().source
            deleteEpisodes(entry.value.map { it.episode }, anime, source, true)
        }
    }

    /**
     * Deletes the directories of a list of downloaded episodes.
     *
     * @param episodes the list of episodes to delete.
     * @param anime the anime of the episodes.
     * @param source the source of the episodes.
     */
    fun deleteEpisodes(episodes: List<Episode>, anime: Anime, source: AnimeSource, isCancelling: Boolean = false): List<Episode> {
        val filteredEpisodes = if (isCancelling) {
            episodes
        } else {
            getEpisodesToDelete(episodes, anime)
        }
        launchIO {
            removeFromDownloadQueue(filteredEpisodes)

            val episodeDirs = provider.findEpisodeDirs(filteredEpisodes, anime, source)
            episodeDirs.forEach { it.delete() }
            cache.removeEpisodes(filteredEpisodes, anime)
            if (cache.getDownloadCount(anime) == 0) { // Delete anime directory if empty
                episodeDirs.firstOrNull()?.parentFile?.delete()
            }
        }
        return filteredEpisodes
    }

    private fun removeFromDownloadQueue(episodes: List<Episode>) {
        val wasRunning = downloader.isRunning
        if (wasRunning) {
            downloader.pause()
        }

        downloader.queue.remove(episodes)

        if (wasRunning) {
            if (downloader.queue.isEmpty()) {
                AnimeDownloadService.stop(context)
                downloader.stop()
            } else if (downloader.queue.isNotEmpty()) {
                downloader.start()
            }
        }
    }

    /**
     * Deletes the directory of a downloaded anime.
     *
     * @param anime the anime to delete.
     * @param source the source of the anime.
     */
    fun deleteAnime(anime: Anime, source: AnimeSource) {
        launchIO {
            downloader.queue.remove(anime)
            provider.findAnimeDir(anime, source)?.delete()
            cache.removeAnime(anime)
        }
    }

    /**
     * Adds a list of episodes to be deleted later.
     *
     * @param episodes the list of episodes to delete.
     * @param anime the anime of the episodes.
     */
    fun enqueueDeleteEpisodes(episodes: List<Episode>, anime: Anime) {
        pendingDeleter.addEpisodes(getEpisodesToDelete(episodes, anime), anime)
    }

    /**
     * Triggers the execution of the deletion of pending episodes.
     */
    fun deletePendingEpisodes() {
        val pendingEpisodes = pendingDeleter.getPendingEpisodes()
        for ((anime, episodes) in pendingEpisodes) {
            val source = sourceManager.get(anime.source) ?: continue
            deleteEpisodes(episodes, anime, source)
        }
    }

    /**
     * Renames an already downloaded episode
     *
     * @param source the source of the anime.
     * @param anime the anime of the episode.
     * @param oldEpisode the existing episode with the old name.
     * @param newEpisode the target episode with the new name.
     */
    fun renameEpisode(source: AnimeSource, anime: Anime, oldEpisode: Episode, newEpisode: Episode) {
        val oldNames = provider.getValidEpisodeDirNames(oldEpisode)
        val newName = provider.getEpisodeDirName(newEpisode)
        val animeDir = provider.getAnimeDir(anime, source)

        // Assume there's only 1 version of the episode name formats present
        val oldFolder = oldNames.asSequence()
            .mapNotNull { animeDir.findFile(it) }
            .firstOrNull()

        if (oldFolder?.renameTo(newName) == true) {
            cache.removeEpisode(oldEpisode, anime)
            cache.addEpisode(newName, animeDir, anime)
        } else {
            logcat(LogPriority.ERROR) { "Could not rename downloaded episode: " + oldNames.joinToString() }
        }
    }

    private fun getEpisodesToDelete(episodes: List<Episode>, anime: Anime): List<Episode> {
        // Retrieve the categories that are set to exclude from being deleted on read
        val categoriesToExclude = preferences.removeExcludeAnimeCategories().get().map(String::toInt)
        val categoriesForAnime = db.getCategoriesForAnime(anime).executeAsBlocking()
            .mapNotNull { it.id }
            .takeUnless { it.isEmpty() }
            ?: listOf(0)

        return if (categoriesForAnime.intersect(categoriesToExclude).isNotEmpty()) {
            episodes.filterNot { it.seen }
        } else if (!preferences.removeBookmarkedChapters()) {
            episodes.filterNot { it.bookmark }
        } else {
            episodes
        }
    }
}
