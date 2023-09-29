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
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.domain.items.episode.model.toSEpisode
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.UnmeteredSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.fetchUrlFromVideo
import eu.kanade.tachiyomi.data.cache.EpisodeCache
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateNotifier
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.saveTo
import eu.kanade.tachiyomi.util.storage.toFFmpegString
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import okhttp3.HttpUrl.Companion.toHttpUrl
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.launchNow
import tachiyomi.core.util.lang.withUIContext
import tachiyomi.core.util.system.ImageUtil
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * This class is the one in charge of downloading episodes.
 *
 * Its queue contains the list of episodes to download. In order to download them, the downloader
 * subscription must be running and the list of episodes must be sent to them by [downloadsRelay].
 *
 * The queue manipulation must be done in one thread (currently the main thread) to avoid unexpected
 * behavior, but it's safe to read it from multiple threads.
 */
class AnimeDownloader(
    private val context: Context,
    private val provider: AnimeDownloadProvider,
    private val cache: AnimeDownloadCache,
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val episodeCache: EpisodeCache = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
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
     * AnimeDownloader subscription.
     */
    private var subscription: Subscription? = null

    /**
     * Relay to send a list of downloads to the downloader.
     */
    private val downloadsRelay = PublishRelay.create<List<AnimeDownload>>()

    /**
     * Preference for user's choice of external downloader
     */
    private val preferences: DownloadPreferences by injectLazy()

    /**
     * Whether the downloader is running.
     */
    val isRunning: Boolean
        get() = subscription != null

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
        launchNow {
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
        if (subscription != null || queueState.value.isEmpty()) {
            return false
        }

        initializeSubscription()

        val pending = queueState.value.filter { it.status != AnimeDownload.State.DOWNLOADED }
        pending.forEach { if (it.status != AnimeDownload.State.QUEUE) it.status = AnimeDownload.State.QUEUE }

        isPaused = false

        downloadsRelay.call(pending)
        return pending.isNotEmpty()
    }

    /**
     * Stops the downloader.
     */
    fun stop(reason: String? = null) {
        destroySubscription()
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

        // Prevent recursion when DownloadService.onDestroy() calls downloader.stop()
        if (AnimeDownloadService.isRunning.value) {
            AnimeDownloadService.stop(context)
        }
    }

    /**
     * Pauses the downloader
     */
    fun pause() {
        destroySubscription()
        queueState.value
            .filter { it.status == AnimeDownload.State.DOWNLOADING }
            .forEach { it.status = AnimeDownload.State.QUEUE }
        isPaused = true
    }

    /**
     * Removes everything from the queue.
     */
    fun clearQueue() {
        destroySubscription()

        _clearQueue()
        notifier.dismissProgress()
    }

    /**
     * Prepares the subscriptions to start downloading.
     */
    private fun initializeSubscription() {
        // Unsubscribe the previous subscription if it exists
        destroySubscription()

        subscription = downloadsRelay.flatMapIterable { it }
            // Concurrently download from 3 different sources
            .groupBy { it.source }
            .flatMap(
                { bySource ->
                    bySource.flatMap(
                        { download ->
                            downloadEpisode(download)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                        },
                        if (sourceManager.get(bySource.key.id) is UnmeteredSource) {
                            downloadPreferences.numberOfDownloads().get()
                        } else {
                            1
                        },
                    )
                },
                5,
            )
            .subscribe(
                { completedDownload ->
                    completeAnimeDownload(completedDownload)
                },
                { error ->
                    logcat(LogPriority.ERROR, error)
                    notifier.onError(error.message)
                    stop()
                },
            )
    }

    /**
     * Destroys the downloader subscriptions.
     */
    private fun destroySubscription() {
        isFFmpegRunning = false
        FFmpegKitConfig.getSessions().filter {
            it.isFFmpeg && (it.state == SessionState.CREATED || it.state == SessionState.RUNNING)
        }.forEach {
            it.cancel()
        }

        subscription?.unsubscribe()
        subscription = null
    }

    /**
     * Creates a download object for every episode and adds them to the downloads queue.
     *
     * @param anime the anime of the episodes to download.
     * @param episodes the list of episodes to download.
     * @param autoStart whether to start the downloader after enqueing the episodes.
     */
    fun queueEpisodes(anime: Anime, episodes: List<Episode>, autoStart: Boolean, changeDownloader: Boolean = false, video: Video? = null) = launchIO {
        if (episodes.isEmpty()) {
            return@launchIO
        }

        val source = sourceManager.get(anime.source) as? AnimeHttpSource ?: return@launchIO
        val wasEmpty = queueState.value.isEmpty()
        // Called in background thread, the operation can be slow with SAF.
        val episodesWithoutDir = async {
            episodes
                // Filter out those already downloaded.
                .filter { provider.findEpisodeDir(it.name, it.scanlator, anime.title, source) == null }
                // Add episodes to queue from the start.
                .sortedByDescending { it.sourceOrder }
        }

        // Runs in main thread (synchronization needed).
        val episodesToQueue = episodesWithoutDir.await()
            // Filter out those already enqueued.
            .filter { episode -> queueState.value.none { it.episode.id == episode.id } }
            // Create a download for each one.
            .map { AnimeDownload(source, anime, it, changeDownloader, video) }

        if (episodesToQueue.isNotEmpty()) {
            addAllToQueue(episodesToQueue)

            if (isRunning) {
                // Send the list of downloads to the downloader.
                downloadsRelay.call(episodesToQueue)
            }

            // Start downloader if needed
            if (autoStart && wasEmpty) {
                val queuedDownloads = queueState.value.count { it: AnimeDownload -> it.source !is UnmeteredSource }
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
                    withUIContext {
                        notifier.onWarning(
                            context.getString(R.string.download_queue_size_warning),
                            WARNING_NOTIF_TIMEOUT_MS,
                            NotificationHandler.openUrl(context, AnimeLibraryUpdateNotifier.HELP_WARNING_URL),
                        )
                    }
                }
                AnimeDownloadService.start(context)
            }
        }
    }

    /**
     * Returns the observable which downloads an episode.
     *
     * @param download the episode to be downloaded.
     */
    private fun downloadEpisode(download: AnimeDownload): Observable<AnimeDownload> = Observable.defer {
        val animeDir = provider.getAnimeDir(download.anime.title, download.source)

        val availSpace = DiskUtil.getAvailableStorageSpace(animeDir)
        if (availSpace != -1L && availSpace < MIN_DISK_SPACE) {
            download.status = AnimeDownload.State.ERROR
            notifier.onError(context.getString(R.string.download_insufficient_space), download.episode.name, download.anime.title)
            return@defer Observable.just(download)
        }

        val episodeDirname = provider.getEpisodeDirName(download.episode.name, download.episode.scanlator)
        val tmpDir = animeDir.createDirectory(episodeDirname + TMP_DIR_SUFFIX)
        notifier.onProgressChange(download)

        val videoObservable = if (download.video == null) {
            // Pull video from network and add them to download object
            download.source.fetchVideoList(download.episode.toSEpisode()).map { it.first() }
                .doOnNext { video ->
                    if (video == null) {
                        throw Exception(context.getString(R.string.video_list_empty_error))
                    }
                    download.video = video
                }
        } else {
            // Or if the video already exists, start from the file
            Observable.just(download.video!!)
        }

        videoObservable
            .doOnNext { _ ->
                if (download.video?.bytesDownloaded == 0L) {
                    // Delete all temporary (unfinished) files
                    tmpDir.listFiles()
                        ?.filter { it.name!!.endsWith(".tmp") }
                        ?.forEach { it.delete() }
                }

                download.downloadedImages = 0
                download.status = AnimeDownload.State.DOWNLOADING
            }
            // Get all the URLs to the source images, fetch pages if necessary
            .flatMap { download.source.fetchUrlFromVideo(it) }
            .doOnNext {
                Observable.interval(50, TimeUnit.MILLISECONDS)
                    // Get the sum of percentages for all the pages.
                    .flatMap {
                        Observable.just(download.video)
                            .flatMap { Observable.just(it!!.progress) }
                    }
                    .takeUntil { download.status != AnimeDownload.State.DOWNLOADING }
                    // Keep only the latest emission to avoid backpressure.
                    .onBackpressureLatest()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { progress ->
                        // Update the view only if the progress has changed.
                        if (download.totalProgress != progress) {
                            download.totalProgress = progress
                            notifier.onProgressChange(download)
                        }
                    }
            }
            // Start downloading videos, consider we can have downloaded images already
            // Concurrently do 5 videos at a time (a fossil of the manga downloads)
            .flatMap({ video -> getOrAnimeDownloadVideo(video, download, tmpDir) }, 5)
            .onBackpressureLatest()
            // Do when video is downloaded.
            .toList()
            .map { download }
            // Do after download completes
            .doOnNext {
                ensureSuccessfulAnimeDownload(download, animeDir, tmpDir, episodeDirname)
                if (download.status == AnimeDownload.State.DOWNLOADED) notifier.dismissProgress()
            }
            // If the video list threw, it will resume here
            .onErrorReturn { error ->
                download.status = AnimeDownload.State.ERROR
                notifier.onError(error.message, download.episode.name, download.anime.title)
                download
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
    private fun getOrAnimeDownloadVideo(video: Video, download: AnimeDownload, tmpDir: UniFile): Observable<Video> {
        // If the video URL is empty, do nothing
        if (video.videoUrl == null) {
            return Observable.just(video)
        }

        val filename = DiskUtil.buildValidFilename(download.episode.name)

        if (video.bytesDownloaded == 0L) {
            val tmpFile = tmpDir.findFile("$filename.tmp")

            // Delete temp file if it exists
            tmpFile?.delete()
        }

        // Try to find the video file
        val videoFile = tmpDir.listFiles()?.firstOrNull { it.name!!.startsWith("$filename.") }

        // If the video is already downloaded, do nothing. Otherwise download from network
        val pageObservable = when {
            videoFile != null -> Observable.just(videoFile)
            episodeCache.isImageInCache(video.videoUrl!!) -> copyVideoFromCache(episodeCache.getVideoFile(video.videoUrl!!), tmpDir, filename)
            else -> {
                if (preferences.useExternalDownloader().get() == download.changeDownloader) {
                    downloadVideo(video, download, tmpDir, filename)
                } else {
                    val betterFileName = DiskUtil.buildValidFilename("${download.anime.title} - ${download.episode.name}")
                    downloadVideoExternal(video, download.source, tmpDir, betterFileName)
                }
            }
        }

        return pageObservable
            // When the video is ready, set image path, progress (just in case) and status
            .doOnNext { file ->
                video.videoUrl = file.uri.path
                video.progress = 100
                download.downloadedImages++
                video.status = Video.State.READY
            }
            .map { video }
            // Mark this video as error and allow to download the remaining
            .onErrorReturn {
                video.progress = 0
                video.status = Video.State.ERROR
                notifier.onError(it.message, download.episode.name, download.anime.title)
                video
            }
    }

    /**
     * Returns the observable which downloads the video from network.
     *
     * @param video the video to download.
     * @param download the AnimeDownload.
     * @param tmpDir the temporary directory of the download.
     * @param filename the filename of the video.
     */
    private fun downloadVideo(video: Video, download: AnimeDownload, tmpDir: UniFile, filename: String): Observable<UniFile> {
        video.status = Video.State.DOWNLOAD_IMAGE
        video.progress = 0
        var tries = 0
        return newObservable(video, download, tmpDir, filename)
            // Retry 3 times, waiting 2, 4 and 8 seconds between attempts.
            .onErrorResumeNext {
                if (tries >= 2) {
                    return@onErrorResumeNext Observable.error(it)
                }
                tries++
                return@onErrorResumeNext Observable.timer((2 shl tries - 1) * 1000L, TimeUnit.MILLISECONDS)
                    .flatMap { newObservable(video, download, tmpDir, filename) }
            }
    }

    private fun isMpd(video: Video): Boolean {
        return video.videoUrl?.toHttpUrl()?.encodedPath?.endsWith(".mpd") ?: false
    }

    private fun isHls(video: Video): Boolean {
        return video.videoUrl?.toHttpUrl()?.encodedPath?.endsWith(".m3u8") ?: false
    }

    private fun ffmpegObservable(video: Video, download: AnimeDownload, tmpDir: UniFile, filename: String): Observable<UniFile> {
        isFFmpegRunning = true
        val headers = video.headers ?: download.source.headers
        val headerOptions = headers.joinToString("", "-headers '", "'") {
            "${it.first}: ${it.second}\r\n"
        }
        val videoFile = tmpDir.findFile("$filename.tmp")
            ?: tmpDir.createFile("$filename.tmp")!!
        val ffmpegFilename = { videoFile.uri.toFFmpegString(context) }

        val ffmpegOptions = getFFmpegOptions(video, headerOptions, ffmpegFilename())

        val ffprobeCommand = { file: String, ffprobeHeaders: String? ->
            FFmpegKitConfig.parseArguments("${ffprobeHeaders?.plus(" ") ?: ""}-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 \"$file\"")
        }

        var duration = 0L
        val logCallback = LogCallback { log ->
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
        return Observable.just(session)
            .map {
                val file = tmpDir.findFile("$filename.tmp")
                file?.renameTo("$filename.mkv")
                file ?: throw Exception("Downloaded file not found")
            }
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

    private fun newObservable(video: Video, download: AnimeDownload, tmpDir: UniFile, filename: String): Observable<UniFile> {
        return if (isHls(video) || isMpd(video)) {
            ffmpegObservable(video, download, tmpDir, filename)
        } else {
            download.source.fetchVideo(video)
                .map { response ->
                    val file = tmpDir.findFile("$filename.tmp") ?: tmpDir.createFile("$filename.tmp")
                    try {
                        response.body.source().saveTo(file.openOutputStream(true))
                        // val extension = getImageExtension(response, file)
                        // TODO: support other file formats!!
                        file.renameTo("$filename.mp4")
                    } catch (e: Exception) {
                        response.close()
                        if (!queueState.value.equals(download)) file.delete()
                        // file.delete()
                        throw e
                    }
                    file
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
    private fun downloadVideoExternal(video: Video, source: AnimeHttpSource, tmpDir: UniFile, filename: String): Observable<UniFile> {
        video.status = Video.State.DOWNLOAD_IMAGE
        video.progress = 0
        return Observable.just(tmpDir.createFile("$filename.mp4")).map {
            try {
                // TODO: support other file formats!!
                // start download with intent
                val pm = context.packageManager
                val pkgName = preferences.externalDownloaderSelection().get()
                val intent: Intent
                if (!pkgName.isNullOrEmpty()) {
                    intent = pm.getLaunchIntentForPackage(pkgName)!!
                    when {
                        // 1DM
                        pkgName.startsWith("idm.internet.download.manager") -> {
                            intent.apply {
                                component = ComponentName(pkgName, "idm.internet.download.manager.Downloader")
                                action = Intent.ACTION_VIEW
                                data = Uri.parse(video.videoUrl)
                                putExtra("extra_filename", filename)
                            }
                        }
                        // ADM
                        pkgName.startsWith("com.dv.adm") -> {
                            val headers = (video.headers ?: source.headers).toList()
                            val bundle = Bundle()
                            headers.forEach { a -> bundle.putString(a.first, a.second.replace("http", "h_ttp")) }

                            intent.apply {
                                component = ComponentName(pkgName, "$pkgName.AEditor")
                                action = Intent.ACTION_VIEW
                                putExtra("com.dv.get.ACTION_LIST_ADD", "${Uri.parse(video.videoUrl)}<info>$filename.mp4")
                                putExtra("com.dv.get.ACTION_LIST_PATH", tmpDir.filePath!!.substringBeforeLast("_"))
                                putExtra("android.media.intent.extra.HTTP_HEADERS", bundle)
                            }
                            it.delete()
                            tmpDir.delete()
                            queueState.value.find { Anime -> Anime.video == video }?.let { Anime ->
                                Anime.status = AnimeDownload.State.DOWNLOADED
                                completeAnimeDownload(Anime)
                            }
                        }
                    }
                } else {
                    intent = Intent(Intent.ACTION_VIEW)
                    intent.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        setDataAndType(Uri.parse(video.videoUrl), "video/*")
                        putExtra("extra_filename", filename)
                    }
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                it.delete()
                throw e
            }
            it
        }
    }

    /**
     * Return the observable which copies the video from cache.
     *
     * @param cacheFile the file from cache.
     * @param tmpDir the temporary directory of the download.
     * @param filename the filename of the video.
     */
    private fun copyVideoFromCache(cacheFile: File, tmpDir: UniFile, filename: String): Observable<UniFile> {
        return Observable.just(cacheFile).map {
            val tmpFile = tmpDir.createFile("$filename.tmp")
            cacheFile.inputStream().use { input ->
                tmpFile.openOutputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val extension = ImageUtil.findImageType(cacheFile.inputStream()) ?: return@map tmpFile
            tmpFile.renameTo("$filename.${extension.extension}")
            cacheFile.delete()
            tmpFile
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
        val downloadedVideo = tmpDir.listFiles().orEmpty().filterNot { it.name!!.endsWith(".tmp") }

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
     * Completes a download. This method is called in the main thread.
     */
    private fun completeAnimeDownload(download: AnimeDownload) {
        // Delete successful downloads from queue
        if (download.status == AnimeDownload.State.DOWNLOADED) {
            // Remove downloaded episode from queue
            removeFromQueue(download)
        }
        if (areAllAnimeDownloadsFinished()) {
            stop()
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

    fun removeFromQueue(episodes: List<Episode>) {
        episodes.forEach { episode ->
            queueState.value.find { it.episode.id == episode.id }?.let { removeFromQueue(it) }
        }
    }

    fun removeFromQueue(anime: Anime) {
        queueState.value.filter { it.anime.id == anime.id }.forEach { removeFromQueue(it) }
    }

    private fun _clearQueue() {
        _queueState.update {
            it.forEach { download ->
                download.progressSubject = null
                download.progressCallback = null
                if (download.status == AnimeDownload.State.DOWNLOADING || download.status == AnimeDownload.State.QUEUE) {
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
