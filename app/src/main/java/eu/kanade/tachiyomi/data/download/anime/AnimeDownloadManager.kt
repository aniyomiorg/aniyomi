package eu.kanade.tachiyomi.data.download.anime

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import rx.Observable
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This class is used to manage episode downloads in the application. It must be instantiated once
 * and retrieved through dependency injection. You can use this class to queue new episodes or query
 * downloaded episodes.
 */
class AnimeDownloadManager(
    private val context: Context,
    private val provider: AnimeDownloadProvider = Injekt.get(),
    private val cache: AnimeDownloadCache = Injekt.get(),
    private val getCategories: GetAnimeCategories = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
) {

    /**
     * Downloader whose only task is to download episodes.
     */
    private val downloader = AnimeDownloader(context, provider, cache, sourceManager)

    /**
     * Queue to delay the deletion of a list of episodes until triggered.
     */
    private val pendingDeleter = AnimeDownloadPendingDeleter(context)

    val queueState
        get() = downloader.queueState

    // For use by DownloadService only
    fun downloaderStart() = downloader.start()
    fun downloaderStop(reason: String? = null) = downloader.stop(reason)

    val isDownloaderRunning
        get() = AnimeDownloadService.isRunning

    /**
     * Tells the downloader to begin downloads.
     */
    fun startDownloads() {
        AnimeDownloadService.start(context)
    }

    /**
     * Tells the downloader to pause downloads.
     */
    fun pauseDownloads() {
        downloader.pause()
        downloader.stop()
    }

    /**
     * Empties the download queue.
     */
    fun clearQueue() {
        downloader.clearQueue()
        downloader.stop()
    }

    /**
     * Returns the download from queue if the episode is queued for download
     * else it will return null which means that the episode is not queued for download
     *
     * @param episodeId the episode to check.
     */
    fun getQueuedDownloadOrNull(episodeId: Long): AnimeDownload? {
        return queueState.value.find { it.episode.id == episodeId }
    }

    fun startDownloadNow(episodeId: Long?) {
        if (episodeId == null) return
        val download = getQueuedDownloadOrNull(episodeId)
        // If not in queue try to start a new download
        val toAdd = download ?: runBlocking { AnimeDownload.fromEpisodeId(episodeId) } ?: return
        val queue = queueState.value.toMutableList()
        download?.let { queue.remove(it) }
        queue.add(0, toAdd)
        reorderQueue(queue)
        if (!downloader.isRunning) {
            if (AnimeDownloadService.isRunning(context)) {
                downloader.start()
            } else {
                AnimeDownloadService.start(context)
            }
        }
    }

    /**
     * Reorders the download queue.
     *
     * @param downloads value to set the download queue to
     */
    fun reorderQueue(downloads: List<AnimeDownload>) {
        downloader.updateQueue(downloads)
    }

    /**
     * Tells the downloader to enqueue the given list of episodes.
     *
     * @param anime the anime of the episodes.
     * @param episodes the list of episodes to enqueue.
     * @param autoStart whether to start the downloader after enqueuing the episodes.
     * @param alt whether to use the alternative downloader
     */
    fun downloadEpisodes(anime: Anime, episodes: List<Episode>, autoStart: Boolean = true, alt: Boolean = false, video: Video? = null) {
        downloader.queueEpisodes(anime, episodes, autoStart, alt, video)
    }

    /**
     * Tells the downloader to enqueue the given list of downloads at the start of the queue.
     *
     * @param downloads the list of downloads to enqueue.
     */
    fun addDownloadsToStartOfQueue(downloads: List<AnimeDownload>) {
        if (downloads.isEmpty()) return
        queueState.value.toMutableList().apply {
            addAll(0, downloads)
            reorderQueue(this)
        }
        if (!AnimeDownloadService.isRunning(context)) AnimeDownloadService.start(context)
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
        val episodeDir = provider.findEpisodeDir(episode.name, episode.scanlator, anime.title, source)
        return Observable.fromCallable {
            val files = episodeDir?.listFiles().orEmpty()
                .filter { "video" in it.type.orEmpty() }

            if (files.isEmpty()) {
                throw Exception(context.getString(R.string.video_list_empty_error))
            }

            val file = files[0]
            Video(file.uri.toString(), "download: " + file.uri.toString(), file.uri.toString(), file.uri).apply { status = Video.State.READY }
        }
    }

    /**
     * Returns true if the episode is downloaded.
     *
     * @param episodeName the name of the episode to query.
     * @param episodeScanlator scanlator of the episode to query
     * @param animeTitle the title of the anime to query.
     * @param sourceId the id of the source of the episode.
     * @param skipCache whether to skip the directory cache and check in the filesystem.
     */
    fun isEpisodeDownloaded(
        episodeName: String,
        episodeScanlator: String?,
        animeTitle: String,
        sourceId: Long,
        skipCache: Boolean = false,
    ): Boolean {
        return cache.isEpisodeDownloaded(episodeName, episodeScanlator, animeTitle, sourceId, skipCache)
    }

    /**
     * Returns the amount of downloaded episodes.
     */
    fun getDownloadCount(): Int {
        return cache.getTotalDownloadCount()
    }

    /**
     * Returns the amount of downloaded episodes for an anime.
     *
     * @param anime the anime to check.
     */
    fun getDownloadCount(anime: Anime): Int {
        return cache.getDownloadCount(anime)
    }

    fun cancelQueuedDownloads(downloads: List<AnimeDownload>) {
        removeFromDownloadQueue(downloads.map { it.episode })
    }

    /**
     * Deletes the directories of a list of downloaded episodes.
     *
     * @param episodes the list of episodes to delete.
     * @param anime the anime of the episodes.
     * @param source the source of the episodes.
     */
    fun deleteEpisodes(episodes: List<Episode>, anime: Anime, source: AnimeSource) {
        val filteredEpisodes = getEpisodesToDelete(episodes, anime)
        if (filteredEpisodes.isNotEmpty()) {
            launchIO {
                removeFromDownloadQueue(filteredEpisodes)

                val (animeDir, episodeDirs) = provider.findEpisodeDirs(filteredEpisodes, anime, source)
                episodeDirs.forEach { it.delete() }
                cache.removeEpisodes(filteredEpisodes, anime)

                // Delete anime directory if empty
                if (animeDir?.listFiles()?.isEmpty() == true) {
                    deleteAnime(anime, source, removeQueued = false)
                }
            }
        }
    }

    /**
     * Deletes the directory of a downloaded anime.
     *
     * @param anime the anime to delete.
     * @param source the source of the anime.
     * @param removeQueued whether to also remove queued downloads.
     */
    fun deleteAnime(anime: Anime, source: AnimeSource, removeQueued: Boolean = true) {
        launchIO {
            if (removeQueued) {
                downloader.removeFromQueue(anime)
            }
            provider.findAnimeDir(anime.title, source)?.delete()
            cache.removeAnime(anime)
            // Delete source directory if empty
            val sourceDir = provider.findSourceDir(source)
            if (sourceDir?.listFiles()?.isEmpty() == true) {
                sourceDir.delete()
                cache.removeSource(source)
            }
        }
    }

    private fun removeFromDownloadQueue(episodes: List<Episode>) {
        val wasRunning = downloader.isRunning
        if (wasRunning) {
            downloader.pause()
        }

        downloader.removeFromQueue(episodes)

        if (wasRunning) {
            if (queueState.value.isEmpty()) {
                downloader.stop()
            } else if (queueState.value.isNotEmpty()) {
                downloader.start()
            }
        }
    }

    /**
     * Adds a list of episodes to be deleted later.
     *
     * @param episodes the list of episodes to delete.
     * @param anime the anime of the episodes.
     */
    fun enqueueEpisodesToDelete(episodes: List<Episode>, anime: Anime) {
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
     * Renames source download folder
     *
     * @param oldSource the old source.
     * @param newSource the new source.
     */
    fun renameSource(oldSource: AnimeSource, newSource: AnimeSource) {
        val oldFolder = provider.findSourceDir(oldSource) ?: return
        val newName = provider.getSourceDirName(newSource)

        val capitalizationChanged = oldFolder.name.equals(newName, ignoreCase = true)
        if (capitalizationChanged) {
            val tempName = newName + "_tmp"
            if (oldFolder.renameTo(tempName).not()) {
                logcat(LogPriority.ERROR) { "Failed to rename source download folder: ${oldFolder.name}" }
                return
            }
        }

        if (oldFolder.renameTo(newName).not()) {
            logcat(LogPriority.ERROR) { "Failed to rename source download folder: ${oldFolder.name}" }
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
        val oldNames = provider.getValidEpisodeDirNames(oldEpisode.name, oldEpisode.scanlator)
        val animeDir = provider.getAnimeDir(anime.title, source)

        // Assume there's only 1 version of the episode name formats present
        val oldFolder = oldNames.asSequence()
            .mapNotNull { animeDir.findFile(it) }
            .firstOrNull()

        val newName = provider.getEpisodeDirName(newEpisode.name, newEpisode.scanlator)
        if (oldFolder?.renameTo(newName) == true) {
            cache.removeEpisode(oldEpisode, anime)
            cache.addEpisode(newName, animeDir, anime)
        } else {
            logcat(LogPriority.ERROR) { "Could not rename downloaded episode: ${oldNames.joinToString()}" }
        }
    }

    private fun getEpisodesToDelete(episodes: List<Episode>, anime: Anime): List<Episode> {
        // Retrieve the categories that are set to exclude from being deleted on read
        val categoriesToExclude = downloadPreferences.removeExcludeAnimeCategories().get().map(String::toLong)

        val categoriesForAnime = runBlocking { getCategories.await(anime.id) }
            .map { it.id }
            .takeUnless { it.isEmpty() }
            ?: listOf(0)

        return if (categoriesForAnime.intersect(categoriesToExclude).isNotEmpty()) {
            episodes.filterNot { it.seen }
        } else if (!downloadPreferences.removeBookmarkedChapters().get()) {
            episodes.filterNot { it.bookmark }
        } else {
            episodes
        }
    }

    fun statusFlow(): Flow<AnimeDownload> = queueState
        .flatMapLatest { downloads ->
            downloads
                .map { download ->
                    download.statusFlow.drop(1).map { download }
                }
                .merge()
        }
        .onStart {
            emitAll(
                queueState.value.filter { download -> download.status == AnimeDownload.State.DOWNLOADING }.asFlow(),
            )
        }

    fun progressFlow(): Flow<AnimeDownload> = queueState
        .flatMapLatest { downloads ->
            downloads
                .map { download ->
                    download.progressFlow.drop(1).map { download }
                }
                .merge()
        }
        .onStart {
            emitAll(
                queueState.value.filter { download -> download.status == AnimeDownload.State.DOWNLOADING }
                    .asFlow(),
            )
        }
}
