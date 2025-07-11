package eu.kanade.tachiyomi.data.download.anime

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.Level
import com.arthenica.ffmpegkit.LogCallback
import com.arthenica.ffmpegkit.LogRedirectionStrategy
import com.arthenica.ffmpegkit.StatisticsCallback
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.animesource.UnmeteredSource
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateNotifier
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.ui.player.loader.EpisodeLoader
import eu.kanade.tachiyomi.ui.player.loader.HosterLoader
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.toFFmpegString
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.i18n.aniyomi.AYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * This class is the one in charge of downloading episodes.
 *
 * Its queue contains the list of episodes to download. In order to download them, the downloader
 * subscription must be running and the list of episodes must be sent to them by [downloaderJob].
 *
 * The queue manipulation must be done in one thread (currently the main thread) to avoid unexpected
 * behavior, but it's safe to read it from multiple threads.
 */
class AnimeDownloader(
    private val context: Context,
    private val provider: AnimeDownloadProvider,
    private val cache: AnimeDownloadCache,
    private val sourceManager: AnimeSourceManager = Injekt.get(),
) {
    /**
     * Store for persisting downloads across restarts.
     */
    private val store = AnimeDownloadStore(context)

    /**
     * Queue where active downloads are kept.
     */
    private val _queueState = MutableStateFlow<List<AnimeDownload>>(emptyList())
    val queueState = _queueState.asStateFlow()

    /**
     * Notifier for the downloader state and progress.
     */
    private val notifier by lazy { AnimeDownloadNotifier(context) }

    /**
     * Coroutine scope used for download job scheduling
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Job object for download queue management
     */
    private var downloaderJob: Job? = null

    /**
     * Preference for user's choice of external downloader
     */
    private val preferences: DownloadPreferences by injectLazy()

    /**
     * Whether the downloader is running.
     */
    val isRunning: Boolean
        get() = downloaderJob?.isActive ?: false

    init {
        scope.launch {
            val episodes = async { store.restore() }
            addAllToQueue(episodes.await())
        }
    }

    /**
     * Starts the downloader. It doesn't do anything if it's already running or there isn't anything
     * to download.
     *
     * @return true if the downloader is started, false otherwise.
     */
    fun start(): Boolean {
        if (isRunning || queueState.value.isEmpty()) {
            return false
        }

        val pending = queueState.value.filter { it.status != AnimeDownload.State.DOWNLOADED }
        pending.forEach { if (it.status != AnimeDownload.State.QUEUE) it.status = AnimeDownload.State.QUEUE }

        launchDownloaderJob()

        return pending.isNotEmpty()
    }

    /**
     * Stops the downloader.
     */
    fun stop(reason: String? = null) {
        cancelDownloaderJob()
        queueState.value
            .filter { it.status == AnimeDownload.State.DOWNLOADING }
            .forEach { it.status = AnimeDownload.State.ERROR }

        if (reason != null) {
            notifier.onWarning(reason)
            return
        }

        if (queueState.value.isNotEmpty()) {
            notifier.onPaused()
        } else {
            notifier.onComplete()
        }

        AnimeDownloadJob.stop(context)
    }

    /**
     * Pauses the downloader
     */
    fun pause() {
        cancelDownloaderJob()
        queueState.value
            .filter { it.status == AnimeDownload.State.DOWNLOADING }
            .forEach { it.status = AnimeDownload.State.QUEUE }
    }

    /**
     * Removes everything from the queue.
     */
    fun clearQueue() {
        cancelDownloaderJob()

        internalClearQueue()
        notifier.dismissProgress()
    }

    /**
     * Prepares the jobs to start downloading.
     */
    private fun launchDownloaderJob() {
        if (isRunning) return

        downloaderJob = scope.launch {
            val activeDownloadsFlow = queueState.transformLatest { queue ->
                while (true) {
                    val activeDownloads = queue.asSequence()
                        .filter {
                            it.status.value <= AnimeDownload.State.DOWNLOADING.value
                        } // Ignore completed downloads, leave them in the queue
                        .groupBy { it.source }
                        .toList().take(3) // Concurrently download from 5 different sources
                        .map { (_, downloads) -> downloads.first() }
                    emit(activeDownloads)

                    if (activeDownloads.isEmpty()) break

                    // Suspend until a download enters the ERROR state
                    val activeDownloadsErroredFlow =
                        combine(activeDownloads.map(AnimeDownload::statusFlow)) { states ->
                            states.contains(AnimeDownload.State.ERROR)
                        }.filter { it }
                    activeDownloadsErroredFlow.first()
                }

                if (areAllAnimeDownloadsFinished()) stop()
            }.distinctUntilChanged()

            // Use supervisorScope to cancel child jobs when the downloader job is cancelled
            supervisorScope {
                val downloadJobs = mutableMapOf<AnimeDownload, Job>()

                activeDownloadsFlow.collectLatest { activeDownloads ->
                    val downloadJobsToStop = downloadJobs.filter { it.key !in activeDownloads }
                    downloadJobsToStop.forEach { (download, job) ->
                        job.cancel()
                        downloadJobs.remove(download)
                    }

                    val downloadsToStart = activeDownloads.filter { it !in downloadJobs }
                    downloadsToStart.forEach { download ->
                        downloadJobs[download] = launchDownloadJob(download)
                    }
                }
            }
        }
    }

    /**
     * Launch the job responsible for download a single video
     */
    private fun CoroutineScope.launchDownloadJob(download: AnimeDownload) = launchIO {
        // This try-catch manages the job cancellation
        try {
            downloadEpisode(download)

            // Remove successful download from queue
            if (download.status == AnimeDownload.State.DOWNLOADED) {
                removeFromQueue(download)
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            logcat(LogPriority.ERROR, e)
            notifier.onError(e.message)
            stop()
        }
    }

    /**
     * Destroys the downloader subscriptions.
     */
    private fun cancelDownloaderJob() {
        downloaderJob?.cancel()
        downloaderJob = null
    }

    /**
     * Creates a download object for every episode and adds them to the downloads queue.
     *
     * @param anime the anime of the episodes to download.
     * @param episodes the list of episodes to download.
     * @param autoStart whether to start the downloader after enqueing the episodes.
     */
    fun queueEpisodes(
        anime: Anime,
        episodes: List<Episode>,
        autoStart: Boolean,
        changeDownloader: Boolean = false,
        video: Video? = null,
    ) {
        if (episodes.isEmpty()) return

        val source = sourceManager.get(anime.source) as? AnimeHttpSource ?: return
        val wasEmpty = queueState.value.isEmpty()

        val episodesToQueue = episodes.asSequence()
            // Filter out those already downloaded.
            .filter { provider.findEpisodeDir(it.name, it.scanlator, anime.title, source) == null }
            // Add episodes to queue from the start.
            .sortedByDescending { it.sourceOrder }
            // Filter out those already enqueued.
            .filter { episode -> queueState.value.none { it.episode.id == episode.id } }
            // Create a download for each one.
            .map { AnimeDownload(source, anime, it, changeDownloader, video) }
            .toList()

        if (episodesToQueue.isNotEmpty()) {
            addAllToQueue(episodesToQueue)

            // Start downloader if needed
            if (autoStart && wasEmpty) {
                val queuedDownloads =
                    queueState.value.count { it: AnimeDownload -> it.source !is UnmeteredSource }
                val maxDownloadsFromSource = queueState.value
                    .groupBy { it.source }
                    .filterKeys { it !is UnmeteredSource }
                    .maxOfOrNull { it.value.size }
                    ?: 0
                // TODO: show warnings in stable
                if (
                    queuedDownloads > DOWNLOADS_QUEUED_WARNING_THRESHOLD ||
                    maxDownloadsFromSource > EPISODES_PER_SOURCE_QUEUE_WARNING_THRESHOLD
                ) {
                    notifier.onWarning(
                        context.stringResource(AYMR.strings.download_queue_size_warning),
                        WARNING_NOTIF_TIMEOUT_MS,
                        NotificationHandler.openUrl(
                            context,
                            AnimeLibraryUpdateNotifier.HELP_WARNING_URL,
                        ),
                    )
                }
                AnimeDownloadJob.start(context)
            }
        }
    }

    /**
     * Download the video associated with download object
     *
     * @param download the episode to be downloaded.
     */
    private suspend fun downloadEpisode(download: AnimeDownload) {
        val animeDir = provider.getAnimeDir(download.anime.title, download.source)

        val availSpace = DiskUtil.getAvailableStorageSpace(animeDir)
        if (availSpace != -1L && availSpace < MIN_DISK_SPACE) {
            download.status = AnimeDownload.State.ERROR
            notifier.onError(
                context.stringResource(AYMR.strings.download_insufficient_space),
                download.episode.name,
                download.anime.title,
                download.anime.id,
            )
            return
        }

        val episodeDirname = provider.getEpisodeDirName(download.episode.name, download.episode.scanlator)
        val tmpDir = animeDir.createDirectory(episodeDirname + TMP_DIR_SUFFIX)!!

        try {
            if (download.video == null) {
                // Pull video from network and add them to download object
                val hosters = EpisodeLoader.getHosters(download.episode, download.anime, download.source)
                if (hosters.isEmpty()) {
                    throw Exception(context.stringResource(AYMR.strings.video_list_empty_error))
                }
                val bestVideo = HosterLoader.getBestVideo(download.source, hosters)
                    ?: throw Exception(context.stringResource(AYMR.strings.video_list_empty_error))
                download.video = bestVideo
            }

            withIOContext {
                getOrDownloadVideoFile(download, tmpDir)
            }

            if (!isDownloadSuccessful(download, tmpDir)) {
                download.status = AnimeDownload.State.ERROR
                return
            }

            val filename = DiskUtil.buildValidFilename("${download.anime.title} - ${download.episode.name}")
            tmpDir.findFile("${filename}_tmp.mkv")?.delete()
            tmpDir.renameTo(episodeDirname)

            cache.addEpisode(episodeDirname, animeDir, download.anime)

            DiskUtil.createNoMediaFile(tmpDir, context)

            download.status = AnimeDownload.State.DOWNLOADED
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            // If the video threw, it will resume here
            logcat(LogPriority.ERROR, error)
            download.status = AnimeDownload.State.ERROR
            notifier.onError(
                error.message,
                download.episode.name,
                download.anime.title,
                download.anime.id,
            )
        }
    }

    /**
     * Gets the video file if already downloaded, otherwise downloads it
     *
     * @param download the download of the video.
     * @param tmpDir the temporary directory of the download.
     */
    private suspend fun getOrDownloadVideoFile(
        download: AnimeDownload,
        tmpDir: UniFile,
    ) {
        val video = download.video!!

        video.status = Video.State.LOAD_VIDEO

        var progressJob: Job? = null

        // Get filename from download info
        val filename = DiskUtil.buildValidFilename(download.episode.name)

        // Delete temp file if it exists
        tmpDir.findFile("$filename.tmp")?.delete()

        // Try to find the video file
        val videoFile = tmpDir.listFiles()?.firstOrNull { it.name!!.startsWith("$filename.mkv") }

        try {
            // If the video is already downloaded, do nothing. Otherwise download from network
            val file = when {
                videoFile != null -> videoFile
                else -> {
                    notifier.onProgressChange(download)

                    download.status = AnimeDownload.State.DOWNLOADING
                    download.progress = 0

                    // If videoFile is not existing then download it
                    if (preferences.useExternalDownloader().get() == download.changeDownloader) {
                        progressJob = scope.launch {
                            while (download.status == AnimeDownload.State.DOWNLOADING) {
                                delay(50)
                                notifier.onProgressChange(download)
                            }
                        }

                        downloadVideo(download, tmpDir, filename)
                    } else {
                        val betterFileName = DiskUtil.buildValidFilename(
                            "${download.anime.title} - ${download.episode.name}",
                        )
                        downloadVideoExternal(download.video!!, download.source, tmpDir, betterFileName)
                    }
                }
            }

            video.videoUrl = file.uri.path ?: ""
            download.progress = 100
            video.status = Video.State.READY
            progressJob?.cancel()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            video.status = Video.State.ERROR
            notifier.onError(e.message, download.episode.name, download.anime.title, download.anime.id)
            progressJob?.cancel()
        }
    }

    /**
     * Define a retry routine in order to accommodate some errors that can be raised
     *
     * @param download the download reference
     * @param tmpDir the directory where placing the file
     * @param filename the name to give to download file
     */
    private suspend fun downloadVideo(
        download: AnimeDownload,
        tmpDir: UniFile,
        filename: String,
    ): UniFile {
        return flow {
            tmpDir.findFile("$filename.tmp")?.delete()
            val videoFile = tmpDir.createFile("$filename.tmp")!!
            try {
                ffmpegDownload(download, tmpDir, videoFile, filename)
            } catch (e: Exception) {
                videoFile.delete()
                throw e
            }

            emit(videoFile)
        }
            // Retry 3 times, waiting 2, 4 and 8 seconds between attempts.
            .retryWhen { _, attempt ->
                if (attempt < 3) {
                    delay((2L shl attempt.toInt()) * 1000)
                    true
                } else {
                    false
                }
            }
            .flowOn(Dispatchers.IO)
            .first()
    }

    // ffmpeg is always on safe mode
    private suspend fun ffmpegDownload(
        download: AnimeDownload,
        tmpDir: UniFile,
        videoFile: UniFile,
        filename: String,
    ) {
        val video = download.video!!

        val ffmpegFilename = { videoFile.uri.toFFmpegString(context) }

        val headers = video.headers ?: download.source.headers
        val headerOptions = headers.joinToString("", "-headers '", "'") {
            "${it.first}: ${it.second}\r\n"
        }

        FFmpegKitConfig.setLogRedirectionStrategy(LogRedirectionStrategy.ALWAYS_PRINT_LOGS)
        val ffmpegOptions = getFFmpegOptions(video, headerOptions, ffmpegFilename())
        val ffprobeCommand = { file: String, ffprobeHeaders: String? ->
            FFmpegKitConfig.parseArguments(
                "${ffprobeHeaders?.plus(" ") ?: ""}-v quiet -show_entries " +
                    "format=duration -of default=noprint_wrappers=1:nokey=1 \"$file\"",
            )
        }

        var duration = 0L

        val logCallback = LogCallback { log ->
            if (log.level <= Level.AV_LOG_WARNING) {
                log.message?.let {
                    logcat(LogPriority.ERROR) { it }
                }
            }
        }

        val statCallback = StatisticsCallback { s ->
            val outTime = (s.time / 1000.0).toLong()

            if (duration != 0L && outTime > 0) {
                download.progress = (100 * outTime / duration).toInt()
            }
        }

        duration = getDuration(ffprobeCommand(video.videoUrl, headerOptions))?.toLong() ?: 0L

        suspendCancellableCoroutine { continuation ->
            val session = FFmpegKit.executeWithArgumentsAsync(
                ffmpegOptions,
                {
                    if (it.returnCode.isValueSuccess) {
                        tmpDir.findFile("$filename.tmp")?.apply {
                            renameTo("$filename.mkv")
                        }
                        continuation.resume(it)
                    } else {
                        continuation.resumeWithException(Exception("Error in ffmpeg!"))
                    }
                },
                logCallback,
                statCallback,
            )
            continuation.invokeOnCancellation {
                session.cancel()
            }
        }
    }

    private fun getFFmpegOptions(video: Video, headerOptions: String, ffmpegFilename: String): Array<String> {
        fun formatInputs(tracks: List<Track>) = tracks.joinToString(" ", postfix = " ") {
            buildList {
                if (it.url.startsWith("http")) {
                    add(headerOptions)
                }
                add("-i")
                add("\"${it.url}\"")
            }.joinToString(" ")
        }

        fun formatMaps(tracks: List<Track>, type: String, offset: Int = 0) = tracks.indices.joinToString(" ") {
            "-map ${it + 1 + offset}:$type"
        }

        fun formatMetadata(tracks: List<Track>, type: String) = tracks.mapIndexed { i, track ->
            "-metadata:s:$type:$i \"title=${track.lang}\""
        }.joinToString(" ")

        val subtitleInputs = formatInputs(video.subtitleTracks)
        val subtitleMaps = formatMaps(video.subtitleTracks, "s")
        val subtitleMetadata = formatMetadata(video.subtitleTracks, "s")

        val audioInputs = formatInputs(video.audioTracks)
        val audioMaps = formatMaps(video.audioTracks, "a", video.subtitleTracks.size)
        val audioMetadata = formatMetadata(video.audioTracks, "a")

        val sourceStreamOptions = video.ffmpegStreamArgs.joinToString(" ") { (key, value) ->
            "-$key \"$value\""
        }
        val sourceVideoOptions = video.ffmpegVideoArgs.joinToString(" ") { (key, value) ->
            "-$key \"$value\""
        }

        val videoInput = buildList {
            if (video.videoUrl.startsWith("http")) {
                add(headerOptions)
            }
            add(sourceStreamOptions)
            add("-i")
            add("\"${video.videoUrl}\"")
        }.joinToString(" ")

        val command = listOf(
            videoInput, subtitleInputs, audioInputs,
            "-map 0:v", audioMaps, "-map 0:a?", subtitleMaps, "-map 0:s? -map 0:t?",
            "-f matroska -c:a copy -c:v copy -c:s copy",
            subtitleMetadata, audioMetadata, sourceVideoOptions,
            "\"$ffmpegFilename\" -y",
        )
            .filter(String::isNotBlank)
            .joinToString(" ")

        return FFmpegKitConfig.parseArguments(command)
    }

    private suspend fun getDuration(ffprobeCommand: Array<String>): Float? {
        return suspendCancellableCoroutine { continuation ->
            val session = FFprobeKit.executeWithArgumentsAsync(ffprobeCommand) {
                if (it.returnCode.isValueSuccess) {
                    continuation.resume(it)
                } else {
                    continuation.resumeWithException(Exception(it.output))
                }
            }
            continuation.invokeOnCancellation { session.cancel() }
        }.output.toFloatOrNull()
    }

    /**
     * Returns the observable which downloads the video with an external downloader.
     *
     * @param video the video to download.
     * @param source the source of the video.
     * @param tmpDir the temporary directory of the download.
     * @param filename the filename of the video.
     */
    private suspend fun downloadVideoExternal(
        video: Video,
        source: AnimeHttpSource,
        tmpDir: UniFile,
        filename: String,
    ): UniFile {
        try {
            val file = tmpDir.createFile("${filename}_tmp.mkv")!!
            withUIContext {
                context.copyToClipboard("Episode download location", tmpDir.filePath!!.substringBeforeLast("_tmp"))
            }

            // TODO: support other file formats!!
            // start download with intent
            val pm = context.packageManager
            val pkgName = preferences.externalDownloaderSelection().get()
            val intent: Intent
            if (pkgName.isNotEmpty()) {
                intent = pm.getLaunchIntentForPackage(pkgName) ?: throw Exception(
                    "Launch intent not found",
                )
                when {
                    // 1DM
                    pkgName.startsWith("idm.internet.download.manager") -> {
                        val headers = (video.headers ?: source.headers).toMap()
                        val bundle = Bundle()
                        for ((key, value) in headers) {
                            bundle.putString(key, value)
                        }

                        intent.apply {
                            component = ComponentName(
                                pkgName,
                                "idm.internet.download.manager.Downloader",
                            )
                            action = Intent.ACTION_VIEW
                            data = video.videoUrl.toUri()

                            putExtra("extra_filename", "$filename.mkv")
                            putExtra("extra_headers", bundle)
                        }
                    }
                    // ADM
                    pkgName.startsWith("com.dv.adm") -> {
                        val headers = (video.headers ?: source.headers).toList()
                        val bundle = Bundle()
                        headers.forEach { a ->
                            bundle.putString(
                                a.first,
                                a.second.replace("http", "h_ttp"),
                            )
                        }

                        intent.apply {
                            component = ComponentName(pkgName, "$pkgName.AEditor")
                            action = Intent.ACTION_VIEW
                            putExtra(
                                "com.dv.get.ACTION_LIST_ADD",
                                "${video.videoUrl.toUri()}<info>$filename.mkv",
                            )
                            putExtra(
                                "com.dv.get.ACTION_LIST_PATH",
                                tmpDir.filePath!!.substringBeforeLast("_"),
                            )
                            putExtra("android.media.intent.extra.HTTP_HEADERS", bundle)
                        }
                        file.delete()
                        tmpDir.delete()
                        queueState.value.find { anime -> anime.video == video }?.let { download ->
                            download.status = AnimeDownload.State.DOWNLOADED
                            // Delete successful downloads from queue
                            if (download.status == AnimeDownload.State.DOWNLOADED) {
                                // Remove downloaded episode from queue
                                removeFromQueue(download)
                            }
                            if (areAllAnimeDownloadsFinished()) {
                                stop()
                            }
                        }
                    }
                }
            } else {
                intent = Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setDataAndType(video.videoUrl.toUri(), "video/*")
                    putExtra("extra_filename", filename)
                }
            }
            context.startActivity(intent)
            return file
        } catch (e: Exception) {
            tmpDir.findFile("${filename}_tmp.mkv")?.delete()
            throw e
        }
    }

    /**
     * Checks if the download was successful.
     *
     * @param download the download to check.
     * @param tmpDir the directory where the download is currently stored.
     */
    private fun isDownloadSuccessful(
        download: AnimeDownload,
        tmpDir: UniFile,
    ): Boolean {
        val downloadedVideo = tmpDir.listFiles().orEmpty().filterNot { it.extension == ".tmp" }
        return downloadedVideo.size == 1
    }

    /**
     * Checks if the download was successful.
     *
     * @param download the download to check.
     * @param animeDir the anime directory of the download.
     * @param tmpDir the directory where the download is currently stored.
     * @param dirname the real (non temporary) directory name of the download.
     */
    private suspend fun ensureSuccessfulAnimeDownload(
        download: AnimeDownload,
        animeDir: UniFile,
        tmpDir: UniFile,
        dirname: String,
    ) {
        // Ensure that the episode folder has the full video
        val downloadedVideo = tmpDir.listFiles().orEmpty().filterNot { it.extension == ".tmp" }

        download.status = if (downloadedVideo.size == 1) {
            // Only rename the directory if it's downloaded
            val filename = DiskUtil.buildValidFilename("${download.anime.title} - ${download.episode.name}")
            tmpDir.findFile("${filename}_tmp.mkv")?.delete()
            tmpDir.renameTo(dirname)

            cache.addEpisode(dirname, animeDir, download.anime)

            DiskUtil.createNoMediaFile(tmpDir, context)
            AnimeDownload.State.DOWNLOADED
        } else {
            throw Exception("Unable to finalize download")
        }
    }

    /**
     * Returns true if all the queued downloads are in DOWNLOADED or ERROR state.
     */
    private fun areAllAnimeDownloadsFinished(): Boolean {
        return queueState.value.none { it.status.value <= AnimeDownload.State.DOWNLOADING.value }
    }

    private fun addAllToQueue(downloads: List<AnimeDownload>) {
        _queueState.update {
            downloads.forEach { download ->
                download.status = AnimeDownload.State.QUEUE
            }
            store.addAll(downloads)
            it + downloads
        }
    }

    private fun removeFromQueue(download: AnimeDownload) {
        _queueState.update {
            store.remove(download)
            if (download.status == AnimeDownload.State.DOWNLOADING || download.status == AnimeDownload.State.QUEUE) {
                download.status = AnimeDownload.State.NOT_DOWNLOADED
            }
            it - download
        }
    }

    private inline fun removeFromQueueIf(predicate: (AnimeDownload) -> Boolean) {
        _queueState.update { queue ->
            val downloads = queue.filter { predicate(it) }
            store.removeAll(downloads)
            downloads.forEach { download ->
                if (download.status == AnimeDownload.State.DOWNLOADING ||
                    download.status == AnimeDownload.State.QUEUE
                ) {
                    download.status = AnimeDownload.State.NOT_DOWNLOADED
                }
            }
            queue - downloads.toSet()
        }
    }

    fun removeFromQueue(episodes: List<Episode>) {
        val episodeIds = episodes.map { it.id }
        removeFromQueueIf { it.episode.id in episodeIds }
    }

    fun removeFromQueue(anime: Anime) {
        removeFromQueueIf { it.anime.id == anime.id }
    }

    private fun internalClearQueue() {
        _queueState.update {
            it.forEach { download ->
                if (download.status == AnimeDownload.State.DOWNLOADING ||
                    download.status == AnimeDownload.State.QUEUE
                ) {
                    download.status = AnimeDownload.State.NOT_DOWNLOADED
                }
            }
            store.clear()
            emptyList()
        }
    }

    fun updateQueue(downloads: List<AnimeDownload>) {
        if (queueState == downloads) return

        if (downloads.isEmpty()) {
            clearQueue()
            stop()
            return
        }

        val wasRunning = isRunning

        pause()
        internalClearQueue()
        addAllToQueue(downloads)

        if (wasRunning) {
            start()
        }
    }

    companion object {
        const val TMP_DIR_SUFFIX = "_tmp"
        const val WARNING_NOTIF_TIMEOUT_MS = 30_000L
        const val EPISODES_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 10
        private const val DOWNLOADS_QUEUED_WARNING_THRESHOLD = 20
    }
}

// Arbitrary minimum required space to start a download: 200 MB
private const val MIN_DISK_SPACE = 200L * 1024 * 1024
