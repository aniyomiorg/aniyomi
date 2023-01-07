package eu.kanade.tachiyomi.data.animedownload

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
import com.arthenica.ffmpegkit.StatisticsCallback
import com.hippo.unifile.UniFile
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.download.service.DownloadPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.fetchUrlFromVideo
import eu.kanade.tachiyomi.data.animedownload.model.AnimeDownload
import eu.kanade.tachiyomi.data.animedownload.model.AnimeDownloadQueue
import eu.kanade.tachiyomi.data.animelib.AnimelibUpdateNotifier
import eu.kanade.tachiyomi.data.cache.EpisodeCache
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchNow
import eu.kanade.tachiyomi.util.lang.plusAssign
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.saveTo
import eu.kanade.tachiyomi.util.storage.toFFmpegString
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.async
import logcat.LogPriority
import okhttp3.HttpUrl.Companion.toHttpUrl
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * This class is the one in charge of downloading episodes.
 *
 * Its [queue] contains the list of episodes to download. In order to download them, the downloader
 * subscriptions must be running and the list of episodes must be sent to them by [downloadsRelay].
 *
 * The queue manipulation must be done in one thread (currently the main thread) to avoid unexpected
 * behavior, but it's safe to read it from multiple threads.
 *
 * @param context the application context.
 * @param provider the downloads directory provider.
 * @param cache the downloads cache, used to add the downloads to the cache after their completion.
 * @param sourceManager the source manager.
 */
class AnimeDownloader(
    private val context: Context,
    private val provider: AnimeDownloadProvider,
    private val cache: AnimeDownloadCache,
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val episodeCache: EpisodeCache = Injekt.get(),
) {

    /**
     * Store for persisting downloads across restarts.
     */
    private val store = AnimeDownloadStore(context, sourceManager)

    /**
     * Queue where active downloads are kept.
     */
    val queue = AnimeDownloadQueue(store)

    /**
     * Notifier for the downloader state and progress.
     */
    private val notifier by lazy { AnimeDownloadNotifier(context) }

    /**
     * AnimeDownloader subscriptions.
     */
    private val subscriptions = CompositeSubscription()

    /**
     * Relay to send a list of downloads to the downloader.
     */
    private val downloadsRelay = PublishRelay.create<List<AnimeDownload>>()

    /**
     * Relay to subscribe to the downloader status.
     */
    val runningRelay: BehaviorRelay<Boolean> = BehaviorRelay.create(false)

    private val preferences: DownloadPreferences by injectLazy()

    /**
     * Whether the downloader is running.
     */
    @Volatile
    var isRunning: Boolean = false
        private set

    /**
     * Whether FFmpeg is running.
     */
    @Volatile
    var isFFmpegRunning: Boolean = false

    init {
        launchNow {
            val episodes = async { store.restore() }
            queue.addAll(episodes.await())
        }
    }

    /**
     * Starts the downloader. It doesn't do anything if it's already running or there isn't anything
     * to download.
     *
     * @return true if the downloader is started, false otherwise.
     */
    fun start(): Boolean {
        if (isRunning || queue.isEmpty()) {
            return false
        }

        if (!subscriptions.hasSubscriptions()) {
            initializeSubscriptions()
        }

        val pending = queue.filter { it.status != AnimeDownload.State.DOWNLOADED }
        pending.forEach { if (it.status != AnimeDownload.State.QUEUE) it.status = AnimeDownload.State.QUEUE }

        notifier.paused = false

        downloadsRelay.call(pending)
        return pending.isNotEmpty()
    }

    /**
     * Stops the downloader.
     */
    fun stop(reason: String? = null) {
        destroySubscriptions()
        queue
            .filter { it.status == AnimeDownload.State.DOWNLOADING }
            .forEach { it.status = AnimeDownload.State.ERROR }

        if (reason != null) {
            notifier.onWarning(reason)
            return
        }

        if (notifier.paused && !queue.isEmpty()) {
            notifier.onPaused()
        } else {
            notifier.onComplete()
        }

        notifier.paused = false
    }

    /**
     * Pauses the downloader
     */
    fun pause() {
        destroySubscriptions()
        queue
            .filter { it.status == AnimeDownload.State.DOWNLOADING }
            .forEach { it.status = AnimeDownload.State.QUEUE }
        notifier.paused = true
    }

    /**
     * Check if downloader is paused
     */
    fun isPaused() = !isRunning

    /**
     * Removes everything from the queue.
     *
     * @param isNotification value that determines if status is set (needed for view updates)
     */
    fun clearQueue(isNotification: Boolean = false) {
        destroySubscriptions()

        // Needed to update the episode view
        if (isNotification) {
            queue
                .filter { it.status == AnimeDownload.State.QUEUE }
                .forEach {
                    val animeDir = provider.getAnimeDir(it.anime.title, it.source)
                    val episodeDirname = provider.getEpisodeDirName(it.episode.name, it.episode.scanlator)
                    val tmpDir = animeDir.findFile(episodeDirname + TMP_DIR_SUFFIX)
                    tmpDir?.delete()
                    it.status = AnimeDownload.State.NOT_DOWNLOADED
                }
        }
        queue.clear()
        notifier.dismissProgress()
    }

    /**
     * Prepares the subscriptions to start downloading.
     */
    private fun initializeSubscriptions() {
        if (isRunning) return
        isRunning = true
        runningRelay.call(true)

        subscriptions.clear()

        subscriptions += downloadsRelay.concatMapIterable { it }
            // Concurrently download from 5 different sources
            .groupBy { it.source }
            .flatMap(
                { bySource ->
                    bySource.concatMap { download ->
                        downloadEpisode(download).subscribeOn(Schedulers.io())
                    }
                },
                5,
            )
            .onBackpressureLatest()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    completeAnimeDownload(it)
                },
                { error ->
                    AnimeDownloadService.stop(context)
                    logcat(LogPriority.ERROR, error)
                    notifier.onError(error.message)
                },
            )
    }

    /**
     * Destroys the downloader subscriptions.
     */
    private fun destroySubscriptions() {
        if (!isRunning) return
        isRunning = false
        runningRelay.call(false)

        isFFmpegRunning = false
        FFmpegKitConfig.getSessions().filter {
            it.isFFmpeg && (it.state == SessionState.CREATED || it.state == SessionState.RUNNING)
        }.forEach {
            it.cancel()
        }

        subscriptions.clear()
    }

    /**
     * Creates a download object for every episode and adds them to the downloads queue.
     *
     * @param anime the anime of the episodes to download.
     * @param episodes the list of episodes to download.
     * @param autoStart whether to start the downloader after enqueing the episodes.
     */
    fun queueEpisodes(anime: Anime, episodes: List<Episode>, autoStart: Boolean, changeDownloader: Boolean = false) = launchIO {
        if (episodes.isEmpty()) {
            return@launchIO
        }

        val source = sourceManager.get(anime.source) as? AnimeHttpSource ?: return@launchIO
        val wasEmpty = queue.isEmpty()
        // Called in background thread, the operation can be slow with SAF.
        val episodesWithoutDir = async {
            episodes
                // Filter out those already downloaded.
                .filter { provider.findEpisodeDir(it.name, it.scanlator, anime.title, source) == null }
                // Add episodes to queue from the start.
                .sortedByDescending { it.source_order }
        }

        // Runs in main thread (synchronization needed).
        val episodesToQueue = episodesWithoutDir.await()
            // Filter out those already enqueued.
            .filter { episode -> queue.none { it.episode.id == episode.id } }
            // Create a download for each one.
            .map { AnimeDownload(source, anime, it, changeDownloader) }

        if (episodesToQueue.isNotEmpty()) {
            queue.addAll(episodesToQueue)

            if (isRunning) {
                // Send the list of downloads to the downloader.
                downloadsRelay.call(episodesToQueue)
            }

            // Start downloader if needed
            if (autoStart && wasEmpty) {
                val queuedDownloads = queue.filter { it.source !is UnmeteredSource }.count()
                val maxDownloadsFromSource = queue
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
                            NotificationHandler.openUrl(context, AnimelibUpdateNotifier.HELP_WARNING_URL),
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
            notifier.onError(context.getString(R.string.download_insufficient_space), download.episode.name)
            return@defer Observable.just(download)
        }

        val episodeDirname = provider.getEpisodeDirName(download.episode.name, download.episode.scanlator)
        val tmpDir = animeDir.createDirectory(episodeDirname + TMP_DIR_SUFFIX)
        notifier.onProgressChange(download)

        val videoObservable = if (download.video == null) {
            // Pull video from network and add them to download object
            download.source.fetchVideoList(download.episode).map { it.first() }
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
                notifier.onError(error.message, download.episode.name)
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

            // Delete temp file if it exists.
            tmpFile?.delete()
        }

        // Try to find the video file.
        val videoFile = tmpDir.listFiles()!!.find { it.name!!.startsWith("$filename.") }

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
                video.status = Video.READY
            }
            .map { video }
            // Mark this video as error and allow to download the remaining
            .onErrorReturn {
                video.progress = 0
                video.status = Video.ERROR
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
        video.status = Video.DOWNLOAD_IMAGE
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
        var nextLineIsDuration = false
        val logCallback = LogCallback { log ->
            if (nextLineIsDuration) {
                parseDuration(log.message)?.let { duration = it }
                nextLineIsDuration = false
            }
            if (log.message == "  Duration: ") nextLineIsDuration = true
            if (log.level <= Level.AV_LOG_WARNING) log.message?.let { logcat { it } }
        }
        val statisticsCallback = StatisticsCallback {
            if (duration > 0L) {
                video.progress = (100 * it.time.toLong() / duration).toInt()
            }
        }
        val session = FFmpegSession.create(ffmpegOptions, {}, logCallback, statisticsCallback)

        val inputDuration = getDuration(ffprobeCommand(video.videoUrl!!, headerOptions)) ?: 0F
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
                        if (!queue.contains(download)) file.delete()
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
        video.status = Video.DOWNLOAD_IMAGE
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
                                component = ComponentName(pkgName, "${pkgName.substringBeforeLast(".")}.Downloader")
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
                            queue.find { Anime -> Anime.video == video }?.let { Anime ->
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
        // Ensure that the episode folder has all the images.
        val downloadedImages = tmpDir.listFiles().orEmpty().filterNot { it.name!!.endsWith(".tmp") }

        download.status = if (downloadedImages.size == 1) {
            // Only rename the directory if it's downloaded.
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
            // remove downloaded episode from queue
            queue.remove(download)
        }
        if (areAllAnimeDownloadsFinished()) {
            AnimeDownloadService.stop(context)
        }
    }

    /**
     * Returns true if all the queued downloads are in DOWNLOADED or ERROR state.
     */
    private fun areAllAnimeDownloadsFinished(): Boolean {
        return queue.none { it.status.value <= AnimeDownload.State.DOWNLOADING.value }
    }

    companion object {
        const val TMP_DIR_SUFFIX = "_tmp"
        const val WARNING_NOTIF_TIMEOUT_MS = 30_000L
        const val EPISODES_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 15
        private const val DOWNLOADS_QUEUED_WARNING_THRESHOLD = 30
    }
}

// Arbitrary minimum required space to start a download: 200 MB
private const val MIN_DISK_SPACE = 200L * 1024 * 1024
