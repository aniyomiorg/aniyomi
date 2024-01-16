package eu.kanade.tachiyomi.data.download.anime

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFprobeSession
import com.arthenica.ffmpegkit.Level
import com.arthenica.ffmpegkit.LogCallback
import com.arthenica.ffmpegkit.SessionState
import com.hippo.unifile.UniFile
import eu.kanade.domain.items.episode.model.toSEpisode
import eu.kanade.tachiyomi.animesource.UnmeteredSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateNotifier
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.toFFmpegString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.Throttler
import okio.buffer
import okio.sink
import rx.subjects.PublishSubject
import tachiyomi.core.i18n.stringResource
import tachiyomi.core.storage.extension
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

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
     * The throttler used to control the download speed.
     */
    private val throttler = Throttler()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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

    /**
     * Whether the downloader is paused
     */
    @Volatile
    var isPaused: Boolean = false

    /**
     * Whether FFmpeg is running.
     */
    @Volatile
    var isFFmpegRunning: Boolean = false

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

        isPaused = false

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

        if (isPaused && queueState.value.isNotEmpty()) {
            notifier.onPaused()
        } else {
            notifier.onComplete()
        }

        isPaused = false

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
        isPaused = true
    }

    /**
     * Removes everything from the queue.
     */
    fun clearQueue() {
        cancelDownloaderJob()

        _clearQueue()
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

    private fun CoroutineScope.launchDownloadJob(download: AnimeDownload) = launchIO {
        try {
            downloadEpisode(download)

            // Remove successful download from queue
            if (download.status == AnimeDownload.State.DOWNLOADED) {
                removeFromQueue(download)
            }

            if (download.status == AnimeDownload.State.QUEUE) {
                pause()
            }

            if (areAllAnimeDownloadsFinished()) {
                stop()
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
        isFFmpegRunning = false
        FFmpegKitConfig.getSessions().filter {
            it.isFFmpeg && (it.state == SessionState.CREATED || it.state == SessionState.RUNNING)
        }.forEach {
            it.cancel()
        }

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
                        context.stringResource(MR.strings.download_queue_size_warning),
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
     * Returns the observable which downloads an episode.
     *
     * @param download the episode to be downloaded.
     */
    private suspend fun downloadEpisode(download: AnimeDownload) {
        val animeDir = provider.getAnimeDir(download.anime.title, download.source)

        val availSpace = DiskUtil.getAvailableStorageSpace(animeDir)
        if (availSpace != -1L && availSpace < MIN_DISK_SPACE) {
            download.status = AnimeDownload.State.ERROR
            notifier.onError(
                context.stringResource(MR.strings.download_insufficient_space),
                download.episode.name,
                download.anime.title,
            )
            return
        }

        val episodeDirname = provider.getEpisodeDirName(download.episode.name, download.episode.scanlator)
        val tmpDir = animeDir.createDirectory(episodeDirname + TMP_DIR_SUFFIX)!!
        notifier.onProgressChange(download)

        val video = if (download.video == null) {
            // Pull video from network and add them to download object
            try {
                val fetchedVideo = download.source.getVideoList(download.episode.toSEpisode()).first()
                download.video = fetchedVideo
                fetchedVideo
            } catch (e: Exception) {
                throw Exception(context.stringResource(MR.strings.video_list_empty_error))
            }
        } else {
            // Or if the video already exists, return it
            download.video!!
        }

        if (download.video!!.bytesDownloaded == 0L) {
            // Delete all temporary (unfinished) files
            tmpDir.listFiles()
                ?.filter { it.extension == ".tmp" }
                ?.forEach { it.delete() }
        }

        download.downloadedImages = 0
        download.status = AnimeDownload.State.DOWNLOADING

        val progressJob = scope.launch {
            while (download.status == AnimeDownload.State.DOWNLOADING) {
                delay(50)
                val progress = download.video!!.progress
                if (download.totalProgress != progress) {
                    download.totalProgress = progress
                    notifier.onProgressChange(download)
                }
            }
        }

        try {
            // Replace this with your actual download logic
            getOrAnimeDownloadVideo(video, download, tmpDir)
        } catch (e: Exception) {
            download.status = AnimeDownload.State.ERROR
            notifier.onError(e.message, download.episode.name, download.anime.title)
        } finally {
            progressJob.cancel()
        }

        try {
            ensureSuccessfulAnimeDownload(download, animeDir, tmpDir, episodeDirname)
            if (download.status == AnimeDownload.State.DOWNLOADED) {
                notifier.dismissProgress()
            }
        } catch (e: Exception) {
            download.status = AnimeDownload.State.ERROR
            notifier.onError(e.message, download.episode.name, download.anime.title)
        }
    }

    /**
     * Returns the observable which gets the image from the filesystem if it exists or downloads it
     * otherwise.
     *
     * @param video the video to download.
     * @param download the download of the video.
     * @param tmpDir the temporary directory of the download.
     */
    private suspend fun getOrAnimeDownloadVideo(
        video: Video,
        download: AnimeDownload,
        tmpDir: UniFile,
    ): Video {
        // If the video URL is empty, do nothing
        if (video.videoUrl == null) {
            return video
        }

        val filename = DiskUtil.buildValidFilename(download.episode.name)

        if (video.bytesDownloaded == 0L) {
            val tmpFile = tmpDir.findFile("$filename.tmp")

            // Delete temp file if it exists
            tmpFile?.delete()
        }

        val videoFile = tmpDir.listFiles()?.firstOrNull { it.name!!.startsWith("$filename.") }

        // If the video is already downloaded, do nothing. Otherwise download from network
        val file = when {
            videoFile != null -> videoFile
            else -> {
                if (preferences.useExternalDownloader().get() == download.changeDownloader) {
                    downloadVideo(video, download, tmpDir, filename)
                } else {
                    val betterFileName = DiskUtil.buildValidFilename(
                        "${download.anime.title} - ${download.episode.name}",
                    )
                    downloadVideoExternal(video, download.source, tmpDir, betterFileName)
                }
            }
        }

        // When the video is ready, set image path, progress (just in case) and status
        try {
            video.videoUrl = file.uri.path
            video.progress = 100
            download.downloadedImages++
            video.status = Video.State.READY
        } catch (e: Exception) {
            video.progress = 0
            video.status = Video.State.ERROR
            notifier.onError(e.message, download.episode.name, download.anime.title)
        }

        return video
    }

    /**
     * Returns the observable which downloads the video from network.
     *
     * @param video the video to download.
     * @param download the AnimeDownload.
     * @param tmpDir the temporary directory of the download.
     * @param filename the filename of the video.
     */
    private suspend fun downloadVideo(
        video: Video,
        download: AnimeDownload,
        tmpDir: UniFile,
        filename: String,
    ): UniFile {
        video.status = Video.State.DOWNLOAD_IMAGE
        video.progress = 0
        var tries = 0
        var forceSequential = false

        // Define a suspend function to encapsulate the retry logic
        suspend fun attemptDownload(): UniFile {
            return try {
                newDownload(video, download, tmpDir, filename, forceSequential)
            } catch (e: Exception) {
                // If the download failed, try again in sequential mode
                forceSequential = true
                if (tries >= 2) throw e
                tries++
                delay((2 shl (tries - 1)) * 1000L)
                attemptDownload()
            }
        }

        return attemptDownload()
    }

    private fun isMpd(video: Video): Boolean {
        return video.videoUrl?.toHttpUrl()?.encodedPath?.endsWith(".mpd") ?: false
    }

    private fun isHls(video: Video): Boolean {
        return video.videoUrl?.toHttpUrl()?.encodedPath?.endsWith(".m3u8") ?: false
    }

    private suspend fun ffmpegDownload(
        video: Video,
        download: AnimeDownload,
        tmpDir: UniFile,
        filename: String,
    ): UniFile = coroutineScope {
        isFFmpegRunning = true
        val headers = video.headers ?: download.source.headers
        val headerOptions = headers.joinToString("", "-headers '", "'") {
            "${it.first}: ${it.second}\r\n"
        }
        val videoFile = tmpDir.findFile("$filename.tmp") ?: tmpDir.createFile("$filename.tmp")!!
        val ffmpegFilename = { videoFile.uri.toFFmpegString(context) }

        val ffmpegOptions = getFFmpegOptions(video, headerOptions, ffmpegFilename())
        val ffprobeCommand = { file: String, ffprobeHeaders: String? ->
            FFmpegKitConfig.parseArguments(
                "${ffprobeHeaders?.plus(" ") ?: ""}-v error -show_entries " +
                    "format=duration -of default=noprint_wrappers=1:nokey=1 \"$file\"",
            )
        }
        var duration = 0L
        var nextLineIsDuration = false
        val logCallback = LogCallback { log ->
            if (nextLineIsDuration) {
                parseDuration(log.message)?.let { duration = it }
                nextLineIsDuration = false
            }
            if (log.level <= Level.AV_LOG_WARNING) log.message?.let { logcat { it } }
            if (duration != 0L && log.message.startsWith("frame=")) {
                val outTime = log.message
                    .substringAfter("time=", "")
                    .substringBefore(" ", "")
                    .let { parseTimeStringToSeconds(it) }
                if (outTime != null && outTime > 0L) video.progress = (100 * outTime / duration).toInt()
            }
        }
        val session = FFmpegSession.create(ffmpegOptions, {}, logCallback, {})
        val inputDuration = getDuration(ffprobeCommand(video.videoUrl!!, headerOptions)) ?: 0F
        duration = inputDuration.toLong()
        FFmpegKitConfig.ffmpegExecute(session)
        val outputDuration = getDuration(ffprobeCommand(ffmpegFilename(), null)) ?: 0F
        // allow for slight errors
        if (inputDuration > outputDuration * 1.01F) {
            tmpDir.findFile("$filename.tmp")?.delete()
        }
        session.failStackTrace?.let { trace ->
            logcat(LogPriority.ERROR) { trace }
            throw Exception("Error in ffmpeg!")
        }

        val file = tmpDir.findFile("$filename.tmp")?.apply {
            renameTo("$filename.mkv")
        }
        file ?: throw Exception("Downloaded file not found")
    }

    private fun parseTimeStringToSeconds(timeString: String): Long? {
        val parts = timeString.split(":")
        if (parts.size != 3) {
            // Invalid format
            return null
        }

        return try {
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            val secondsAndMilliseconds = parts[2].split(".")
            val seconds = secondsAndMilliseconds[0].toInt()
            val milliseconds = secondsAndMilliseconds[1].toInt()

            (hours * 3600 + minutes * 60 + seconds + milliseconds / 100.0).toLong()
        } catch (e: NumberFormatException) {
            // Invalid number format
            null
        }
    }

    private fun getFFmpegOptions(video: Video, headerOptions: String, ffmpegFilename: String): Array<String> {
        val subtitleInputs = video.subtitleTracks.joinToString(" ", postfix = " ") {
            "-i \"${it.url}\""
        }
        val subtitleMaps = video.subtitleTracks.indices.joinToString(" ") {
            val index = it + 1
            "-map $index:s"
        }
        val subtitleMetadata = video.subtitleTracks.mapIndexed { i, sub ->
            "-metadata:s:s:$i \"title=${sub.lang}\""
        }.joinToString(" ")

        Locale("")
        return FFmpegKitConfig.parseArguments(
            headerOptions +
                " -i \"${video.videoUrl}\" " + subtitleInputs +
                "-map 0:v -map 0:a " + subtitleMaps + " -map 0:s?" +
                " -f matroska -c:a copy -c:v copy -c:s ass " +
                subtitleMetadata +
                " \"$ffmpegFilename\" -y",
        )
    }

    private fun getDuration(ffprobeCommand: Array<String>): Float? {
        val session = FFprobeSession.create(ffprobeCommand)
        FFmpegKitConfig.ffprobeExecute(session)
        return session.allLogsAsString.trim().toFloatOrNull()
    }

    /**
     * Returns the parsed duration in milliseconds
     *
     * @param durationString the string formatted in HOURS:MINUTES:SECONDS.HUNDREDTHS
     */
    private fun parseDuration(durationString: String): Long? {
        val splitString = durationString.split(":")
        if (splitString.lastIndex != 2) return null
        val hours = splitString[0].toLong()
        val minutes = splitString[1].toLong()
        val secondsString = splitString[2].split(".")
        if (secondsString.lastIndex != 1) return null
        val fullSeconds = secondsString[0].toLong()
        val hundredths = secondsString[1].toLong()
        return hours * 3600000L + minutes * 60000L + fullSeconds * 1000L + hundredths * 10L
    }

    private suspend fun multiPartDownload(
        video: Video,
        download: AnimeDownload,
        tmpDir: UniFile,
        filename: String,
    ): UniFile {
        // first we fetch the size of the video
        val size: Long = download.source.getVideoSize(video, 3)
        if (size == -1L) {
            throw Exception("Could not get video size")
        }
        var nParts = preferences.numberOfThreads().get()
        var partSize = size.div(nParts)
        if (partSize < 1024) {
            nParts = size.div(1024).toInt()
            partSize = size.div(nParts)
        }
        if (partSize < 1024) {
            logcat(LogPriority.WARN) {
                "Part size is too small, falling back to sequential download"
            }
            return newDownload(video, download, tmpDir, filename, true)
        }
        val partList = mutableListOf<UniFile>()
        val rangeList = mutableListOf<Array<Long>>()
        val partJobList = mutableListOf<Job>()

        // clear the tmp dir, when pause/resume is implemented, this should be changed, we will need the old files to resume
        tmpDir.listFiles()?.forEach { it.delete() }
        // create the parts as start byte and end byte, when p/r we will need to check if the file exists and resume from there using existing file size
        // a better way would be to create chunk of predefined size and use X workers to download them, but this is easier to implement
        // if we will use the second method pause/resume will be easier to implement since we can discard the partially downloaded chunks without losing too much progress
        for (i in 0 until nParts) {
            val start = i * partSize
            val end =
                if (i == nParts - 1) {
                    size
                } else {
                    (i + 1) * partSize - 1
                }
            rangeList.add(arrayOf(start, end))
            val part = tmpDir.createFile("$filename.part$i.tmp")!!
            partList.add(part)
        }

        var failed = false
        val totalProgresses = mutableListOf<Int>()
        totalProgresses.addAll(List(nParts) { 0 })

        for (range in rangeList) {
            val splitWeight = (range[1] - range[0]).toFloat() / size.toFloat()
            // create a listener for each part, so we can update the progress generally
            val listener =
                object : ProgressListener {
                    override fun update(
                        bytesRead: Long,
                        contentLength: Long,
                        done: Boolean,
                    ) {
                        val progress = (((bytesRead * 90 / (range[1] - range[0])) * splitWeight).toInt())
                        totalProgresses[rangeList.indexOf(range)] = progress
                        video.progress = min(totalProgresses.reduce(Int::plus), 90)
                    }
                }
            throttler.apply {
                bytesPerSecond(preferences.downloadSpeedLimit().get().toLong() * 1024)
            }
            partJobList.add(
                scope.launchIO {
                    try {
                        if (failed) throw Exception("Download failed")
                        // I don't know if this is "pausable", I suspect that will only write the bytes to the file when the download of the segment is complete
                        val response = download.source.getVideoChunk(video, range[0], range[1], listener)
                        val file =
                            tmpDir.findFile("$filename.part${rangeList.indexOf(range)}.tmp")
                                ?: tmpDir.createFile("$filename.part${rangeList.indexOf(range)}.tmp")!!
                        // try to open the file and append the bytes
                        try {
                            response.body.source().use { source ->
                                file.openOutputStream(true).use { output ->
                                    val sink = output.sink().buffer()
                                    val buffer = ByteArray(4 * 1024)
                                    var totalBytesRead = 0L
                                    var bytesRead: Int
                                    val throttledSource = throttler.source(source).buffer()
                                    while (throttledSource.read(buffer).also { bytesRead = it }.toLong() != -1L) {
                                        // Check if the download is paused, if so, wait
                                        while (isPaused) {
                                            delay(1000) // Wait for 1 second before checking again
                                        }
                                        // Write the bytes to the file
                                        sink.write(buffer, 0, bytesRead)
                                        sink.emitCompleteSegments()
                                        totalBytesRead += bytesRead
                                    }
                                    sink.flush()
                                    sink.close()
                                    throttledSource.close()
                                }
                            }
                        } catch (e: Exception) {
                            response.close()
                            failed = true
                            throw e
                        }
                    } catch (e: Exception) {
                        failed = true
                        throw e
                    }
                },
            )
        }

        for (job in partJobList) {
            job.join()
        }
        try {
            val mergeSize = (10.toFloat() / (nParts - 1).toFloat())
            val file0 = tmpDir.findFile("$filename.part0.tmp") ?: tmpDir.createFile("$filename.part0.tmp")!!
            // merge all parts into file0
            for (i in 1 until nParts) {
                val part = tmpDir.findFile("$filename.part$i.tmp")
                    ?: tmpDir.createFile("$filename.part$i.tmp")!!
                part.openInputStream().use { input ->
                    file0.openOutputStream(true).use { output ->
                        input.copyTo(output)
                    }
                }
                part.delete()
                video.progress = 90 + ((mergeSize * i).toInt())
            }
            file0.renameTo("$filename.mp4")
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            failed = true
        }
        if (failed) {
            for (i in 0 until nParts) {
                val part =
                    tmpDir.findFile("$filename.part$i.tmp")
                        ?: tmpDir.createFile("$filename.part$i.tmp")!!
                part.delete()
            }
            throw Exception("Download failed")
        }
        return tmpDir.findFile("$filename.mp4") ?: throw Exception("Download failed")
    }

    private suspend fun newDownload(
        video: Video,
        download: AnimeDownload,
        tmpDir: UniFile,
        filename: String,
        forceSequential: Boolean,
    ): UniFile {
        // Check if the download is paused before starting
        while (isPaused) {
            delay(1000) // This is a pause check delay, adjust the timing as needed.
        }
        when {
            isHls(video) || isMpd(video) -> {
                return ffmpegDownload(video, download, tmpDir, filename)
            }

            preferences.multithreadingDownload().get() && !forceSequential -> {
                return multiPartDownload(video, download, tmpDir, filename)
            }

            else -> {
                val response = download.source.getVideo(video)
                val file = tmpDir.findFile("$filename.tmp") ?: tmpDir.createFile("$filename.tmp")!!
                // Write to file with pause/resume capability
                try {
                    throttler.apply {
                        bytesPerSecond(preferences.downloadSpeedLimit().get().toLong() * 1024, 4096, 8192)
                    }
                    response.body.source().use { source ->
                        file.openOutputStream(true).use { output ->
                            val sink = output.sink().buffer()
                            val buffer = ByteArray(4 * 1024)
                            var totalBytesRead = 0L
                            var bytesRead: Int
                            val throttledSource = throttler.source(source).buffer()
                            while (throttledSource.read(buffer).also { bytesRead = it }.toLong() != -1L) {
                                // Check if the download is paused, if so, wait
                                while (isPaused) {
                                    delay(1000) // Wait for 1 second before checking again
                                }
                                // Write the bytes to the file
                                sink.write(buffer, 0, bytesRead)
                                sink.emitCompleteSegments()
                                totalBytesRead += bytesRead
                                // Update progress here if needed
                            }
                            sink.flush()
                            sink.close()
                            throttledSource.close()
                        }
                    }
                    // After download is complete, rename the file to its final name
                    file.renameTo("$filename.mp4")
                    return file
                } catch (e: Exception) {
                    response.close()
                    if (!queueState.value.equals(download)) file.delete()
                    throw e
                }
            }
        }
    }

    /**
     * Returns the observable which downloads the video with an external downloader.
     *
     * @param video the video to download.
     * @param source the source of the video.
     * @param tmpDir the temporary directory of the download.
     * @param filename the filename of the video.
     */
    private fun downloadVideoExternal(
        video: Video,
        source: AnimeHttpSource,
        tmpDir: UniFile,
        filename: String,
    ): UniFile {
        video.status = Video.State.DOWNLOAD_IMAGE
        video.progress = 0

        try {
            val file = tmpDir.createFile("$filename.mp4")!!

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
                        intent.apply {
                            component = ComponentName(
                                pkgName,
                                "idm.internet.download.manager.Downloader",
                            )
                            action = Intent.ACTION_VIEW
                            data = Uri.parse(video.videoUrl)
                            putExtra("extra_filename", filename)
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
                                "${Uri.parse(video.videoUrl)}<info>$filename.mp4",
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
                    setDataAndType(Uri.parse(video.videoUrl), "video/*")
                    putExtra("extra_filename", filename)
                }
            }
            context.startActivity(intent)
            return file
        } catch (e: Exception) {
            tmpDir.findFile("$filename.mp4")?.delete()
            throw e
        }
    }

    /**
     * Checks if the download was successful.
     *
     * @param download the download to check.
     * @param animeDir the anime directory of the download.
     * @param tmpDir the directory where the download is currently stored.
     * @param dirname the real (non temporary) directory name of the download.
     */
    private fun ensureSuccessfulAnimeDownload(
        download: AnimeDownload,
        animeDir: UniFile,
        tmpDir: UniFile,
        dirname: String,
    ) {
        // Ensure that the episode folder has the full video
        val downloadedVideo = tmpDir.listFiles().orEmpty().filterNot { it.extension == ".tmp" }

        download.status = if (downloadedVideo.size == 1) {
            // Only rename the directory if it's downloaded
            tmpDir.renameTo(dirname)
            cache.addEpisode(dirname, animeDir, download.anime)

            DiskUtil.createNoMediaFile(tmpDir, context)
            AnimeDownload.State.DOWNLOADED
        } else {
            AnimeDownload.State.ERROR
        }
    }

    /**
     * Returns true if all the queued downloads are in DOWNLOADED or ERROR state.
     */
    private fun areAllAnimeDownloadsFinished(): Boolean {
        return queueState.value.none { it.status.value <= AnimeDownload.State.DOWNLOADING.value }
    }

    private val progressSubject = PublishSubject.create<AnimeDownload>()

    private fun setProgressFor(download: AnimeDownload) {
        if (download.status == AnimeDownload.State.DOWNLOADED || download.status == AnimeDownload.State.ERROR) {
            setProgressSubject(download.video, null)
        }
    }

    private fun setProgressSubject(video: Video?, subject: PublishSubject<Video.State>?) {
        video?.progressSubject = subject
    }

    private fun addAllToQueue(downloads: List<AnimeDownload>) {
        _queueState.update {
            downloads.forEach { download ->
                download.progressSubject = progressSubject
                download.progressCallback = ::setProgressFor
                download.status = AnimeDownload.State.QUEUE
            }
            store.addAll(downloads)
            it + downloads
        }
    }

    private fun removeFromQueue(download: AnimeDownload) {
        _queueState.update {
            store.remove(download)
            download.progressSubject = null
            download.progressCallback = null
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

    private fun _clearQueue() {
        _queueState.update {
            it.forEach { download ->
                download.progressSubject = null
                download.progressCallback = null
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
        val wasRunning = isRunning

        if (downloads.isEmpty()) {
            clearQueue()
            stop()
            return
        }

        pause()
        _clearQueue()
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
