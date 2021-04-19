package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.download.model.AnimeDownloadQueue
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.lang.launchIO
import rx.Observable
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

/**
 * This class is used to manage episode downloads in the application. It must be instantiated once
 * and retrieved through dependency injection. You can use this class to queue new episodes or query
 * downloaded episodes.
 *
 * @param context the application context.
 */
class AnimeDownloadManager(private val context: Context) {

    private val sourceManager: SourceManager by injectLazy()
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

    /**
     * Reorders the download queue.
     *
     * @param downloads value to set the download queue to
     */
    fun reorderQueue(downloads: List<AnimeDownload>) {
        val wasRunning = downloader.isRunning

        if (downloads.isEmpty()) {
            DownloadService.stop(context)
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
     * Builds the page list of a downloaded episode.
     *
     * @param source the source of the episode.
     * @param anime the anime of the episode.
     * @param episode the downloaded episode.
     * @return an observable containing the list of pages from the episode.
     */
    fun buildPageList(source: Source, anime: Anime, episode: Episode): Observable<List<Page>> {
        return buildPageList(provider.findEpisodeDir(episode, anime, source))
    }

    /**
     * Builds the page list of a downloaded episode.
     *
     * @param episodeDir the file where the episode is downloaded.
     * @return an observable containing the list of pages from the episode.
     */
    private fun buildPageList(episodeDir: UniFile?): Observable<List<Page>> {
        return Observable.fromCallable {
            val files = episodeDir?.listFiles().orEmpty()
                .filter { "image" in it.type.orEmpty() }

            if (files.isEmpty()) {
                throw Exception(context.getString(R.string.page_list_empty_error))
            }

            files.sortedBy { it.name }
                .mapIndexed { i, file ->
                    Page(i, uri = file.uri).apply { status = Page.READY }
                }
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
        deleteEpisodes(listOf(download.episode), download.anime, download.source)
    }

    /**
     * Deletes the directories of a list of downloaded episodes.
     *
     * @param episodes the list of episodes to delete.
     * @param anime the anime of the episodes.
     * @param source the source of the episodes.
     */
    fun deleteEpisodes(episodes: List<Episode>, anime: Anime, source: Source): List<Episode> {
        val filteredEpisodes = getEpisodesToDelete(episodes)
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
                DownloadService.stop(context)
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
    fun deleteAnime(anime: Anime, source: Source) {
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
        pendingDeleter.addEpisodes(getEpisodesToDelete(episodes), anime)
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
    fun renameEpisode(source: Source, anime: Anime, oldEpisode: Episode, newEpisode: Episode) {
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
            Timber.e("Could not rename downloaded episode: %s.", oldNames.joinToString())
        }
    }

    private fun getEpisodesToDelete(episodes: List<Episode>): List<Episode> {
        return if (!preferences.removeBookmarkedEpisodes()) {
            episodes.filterNot { it.bookmark }
        } else {
            episodes
        }
    }
}
