package eu.kanade.tachiyomi.data.download.anime

import android.app.Application
import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.util.size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.source.local.entries.anime.LocalAnimeSource
import tachiyomi.source.local.io.ArchiveAnime
import tachiyomi.source.local.io.anime.LocalAnimeSourceFileSystem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Cache where we dump the downloads directory from the filesystem. This class is needed because
 * directory checking is expensive and it slows downs the app. The cache is invalidated by the time
 * defined in [renewInterval] as we don't have any control over the filesystem and the user can
 * delete the folders at any time without the app noticing.
 */
class AnimeDownloadCache(
    private val context: Context,
    private val provider: AnimeDownloadProvider = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val extensionManager: AnimeExtensionManager = Injekt.get(),
    private val storageManager: StorageManager = Injekt.get(),
) {

    private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(10))

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow()
        .onStart { emit(Unit) }
        .shareIn(scope, SharingStarted.Lazily, 1)

    /**
     * The interval after which this cache should be invalidated. 1 hour shouldn't cause major
     * issues, as the cache is only used for UI feedback.
     */
    private val renewInterval = 1.hours.inWholeMilliseconds

    /**
     * The last time the cache was refreshed.
     */
    private var lastRenew = 0L
    private var renewalJob: Job? = null

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing = _isInitializing
        .debounce(1000L) // Don't notify if it finishes quickly enough
        .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    private val diskCacheFile: File
        get() = File(context.cacheDir, "dl_anime_index_cache_v3")

    private val localEpisodeCountCacheFile: File
        get() = File(context.cacheDir, "local_episode_cache_v1")

    private val rootDownloadsDirMutex = Mutex()

    private val rootLocalDirMutex = Mutex()
    private var rootDownloadsDir = RootDirectory(storageManager.getDownloadsDirectory())

    private val rootLocalDir = LocalAnimeSourceFileSystem(storageManager)

    /*
     * The Storage Access Framework (SAF) is very slow compared to [import java.io.File], this cache was used to store
     * the count of local chapters. It is only updated when the anime directory is modified, such as renaming the anime,
     * adding or removing chapters
     *
     * Issue related to SAF: https://issuetracker.google.com/issues/130261278
     * */
    private var localEpisodeCountCache = mutableMapOf<String, EpisodeCount>()

    init {
        // Attempt to read cache file
        scope.launch {
            loadDowndloadCountCacheFile()
            loadLocalEpisodeCountCacheFile()
        }

        storageManager.changes
            .onEach { invalidateCache() }
            .launchIn(scope)
    }

    private suspend fun loadDowndloadCountCacheFile() {
        rootDownloadsDirMutex.withLock {
            try {
                if (diskCacheFile.exists()) {
                    val diskCache = diskCacheFile.inputStream().use {
                        ProtoBuf.decodeFromByteArray<RootDirectory>(it.readBytes())
                    }
                    rootDownloadsDir = diskCache
                    lastRenew = System.currentTimeMillis()
                }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e) { "Failed to initialize disk cache" }
                diskCacheFile.delete()
            }
        }
    }

    private suspend fun loadLocalEpisodeCountCacheFile() {
        if (!localEpisodeCountCacheFile.exists()) return
        try {
            localEpisodeCountCache = localEpisodeCountCacheFile.inputStream().use {
                ProtoBuf.decodeFromByteArray<MutableMap<String, EpisodeCount>>(it.readBytes())
            }
            invalidateOutdatedLocalCache()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to initialize from disk cache" }
            localEpisodeCountCacheFile.delete()
        }
    }

    /**
     * Remove cached episode counts for animes whose local folders have been modified,
     * ensuring the cache reflects the current state of the file system.
     */
    private suspend fun invalidateOutdatedLocalCache() {
        rootLocalDirMutex.withLock {
            val outdated = AtomicReference(mutableSetOf<String>())
            // SAF is very slow when obtaining metadata such as lastModified, so this should be done async
            localEpisodeCountCache.map { (key, value) ->
                scope.async {
                    val animeDir = rootLocalDir.getAnimeDirectory(key)
                    if (animeDir != null && value.lastModified >= animeDir.lastModified()) {
                        return@async
                    }
                    outdated.updateAndGet {
                        it.apply { add(key) }
                    }
                }
            }.awaitAll()
            outdated.get().forEach(localEpisodeCountCache::remove)
        }
    }

    /**
     * Returns true if the episode is downloaded.
     *
     * @param episodeName the name of the episode to query.
     * @param episodeScanlator scanlator of the episode to query
     * @param animeTitle the title of the anime to query.
     * @param sourceId the id of the source of the episode.
     */
    fun isEpisodeDownloaded(
        episodeName: String,
        episodeScanlator: String?,
        animeTitle: String,
        sourceId: Long,
        skipCache: Boolean,
    ): Boolean {
        if (skipCache) {
            val source = sourceManager.getOrStub(sourceId)
            return provider.findEpisodeDir(episodeName, episodeScanlator, animeTitle, source) != null
        }

        renewCache()

        val sourceDir = rootDownloadsDir.sourceDirs[sourceId]
        if (sourceDir != null) {
            val animeDir = sourceDir.animeDirs[provider.getAnimeDirName(animeTitle)]
            if (animeDir != null) {
                return provider.getValidEpisodeDirNames(
                    episodeName,
                    episodeScanlator,
                ).any { it in animeDir.episodeDirs }
            }
        }
        return false
    }

    /**
     * Returns the amount of downloaded episodes.
     */
    fun getTotalDownloadCount(): Int {
        renewCache()

        return rootDownloadsDir.sourceDirs.values.sumOf { sourceDir ->
            sourceDir.animeDirs.values.sumOf { animeDir ->
                animeDir.episodeDirs.size
            }
        }
    }

    /**
     * Returns the amount of downloaded episodes for an anime.
     *
     * @param anime the anime to check.
     */
    fun getDownloadCount(anime: Anime): Int {
        if (anime.source == LocalAnimeSource.ID) {
            return runBlocking(Dispatchers.IO) {
                rootLocalDirMutex.withLock {
                    localEpisodeCountCache[anime.url]?.count ?: countLocalEpisodes(anime)
                }
            }
        }

        renewCache()

        val sourceDir = rootDownloadsDir.sourceDirs[anime.source]
        if (sourceDir != null) {
            val animeDir = sourceDir.animeDirs[provider.getAnimeDirName(anime.title)]
            if (animeDir != null) {
                return animeDir.episodeDirs.size
            }
        }
        return 0
    }

    private suspend fun countLocalEpisodes(anime: Anime): Int {
        val animeDir = rootLocalDir.getAnimeDirectory(anime.url) ?: return 0
        return rootLocalDir.getFilesInAnimeDirectory(anime.url)
            .map { scope.async { ArchiveAnime.isSupported(it) } }
            .awaitAll()
            .count { it }
            .also {
                localEpisodeCountCache[anime.url] = EpisodeCount(it, animeDir.lastModified())
            }
    }

    /**
     * Returns the total size of downloaded episodes.
     */
    fun getTotalDownloadSize(): Long {
        renewCache()

        return rootDownloadsDir.sourceDirs.values.sumOf { sourceDir ->
            sourceDir.dir?.size() ?: 0L
        }
    }

    /**
     * Returns the total size of downloaded chapters for an anime.
     *
     * @param anime the anime to check.
     */
    fun getDownloadSize(anime: Anime): Long {
        renewCache()

        return rootDownloadsDir.sourceDirs[anime.source]?.animeDirs?.get(
            provider.getAnimeDirName(
                anime.title,
            ),
        )?.dir?.size() ?: 0
    }

    /**
     * Adds an episode that has just been download to this cache.
     *
     * @param episodeDirName the downloaded episode's directory name.
     * @param animeUniFile the directory of the anime.
     * @param anime the anime of the episode.
     */
    suspend fun addEpisode(episodeDirName: String, animeUniFile: UniFile, anime: Anime) {
        rootDownloadsDirMutex.withLock {
            // Retrieve the cached source directory or cache a new one
            var sourceDir = rootDownloadsDir.sourceDirs[anime.source]
            if (sourceDir == null) {
                val source = sourceManager.get(anime.source) ?: return
                val sourceUniFile = provider.findSourceDir(source) ?: return
                sourceDir = SourceDirectory(sourceUniFile)
                rootDownloadsDir.sourceDirs += anime.source to sourceDir
            }

            // Retrieve the cached anime directory or cache a new one
            val animeDirName = provider.getAnimeDirName(anime.title)
            var animeDir = sourceDir.animeDirs[animeDirName]
            if (animeDir == null) {
                animeDir = AnimeDirectory(animeUniFile)
                sourceDir.animeDirs += animeDirName to animeDir
            }

            // Save the chapter directory
            animeDir.episodeDirs += episodeDirName
        }

        notifyChanges()
    }

    /**
     * Removes an episode that has been deleted from this cache.
     *
     * @param episode the episode to remove.
     * @param anime the anime of the episode.
     */
    suspend fun removeEpisode(episode: Episode, anime: Anime) {
        rootDownloadsDirMutex.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[anime.source] ?: return
            val animeDir = sourceDir.animeDirs[provider.getAnimeDirName(anime.title)] ?: return
            provider.getValidEpisodeDirNames(episode.name, episode.scanlator).forEach {
                if (it in animeDir.episodeDirs) {
                    animeDir.episodeDirs -= it
                }
            }
        }
        rootLocalDirMutex.withLock {
            // Currently, local episode cannot be removed, but I will add this code to sync the cache
            if (anime.source == LocalAnimeSource.ID) {
                localEpisodeCountCache[anime.url]?.count?.dec()
            }
        }

        notifyChanges()
    }

    /**
     * Removes a list of episodes that have been deleted from this cache.
     *
     * @param episodes the list of episode to remove.
     * @param anime the anime of the episode.
     */
    suspend fun removeEpisodes(episodes: List<Episode>, anime: Anime) {
        rootDownloadsDirMutex.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[anime.source] ?: return
            val animeDir = sourceDir.animeDirs[provider.getAnimeDirName(anime.title)] ?: return
            episodes.forEach { episode ->
                provider.getValidEpisodeDirNames(episode.name, episode.scanlator).forEach {
                    if (it in animeDir.episodeDirs) {
                        animeDir.episodeDirs -= it
                    }
                }
            }
        }
        rootLocalDirMutex.withLock {
            if (anime.source == LocalAnimeSource.ID) {
                localEpisodeCountCache[anime.url]?.apply {
                    count = max(count - episodes.size, 0)
                }
            }
        }

        notifyChanges()
    }

    /**
     * Removes an anime that has been deleted from this cache.
     *
     * @param anime the anime to remove.
     */
    suspend fun removeAnime(anime: Anime) {
        rootDownloadsDirMutex.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[anime.source] ?: return
            val animeDirName = provider.getAnimeDirName(anime.title)
            if (sourceDir.animeDirs.containsKey(animeDirName)) {
                sourceDir.animeDirs -= animeDirName
            }
        }
        rootLocalDirMutex.withLock {
            if (anime.source == LocalAnimeSource.ID) {
                localEpisodeCountCache.remove(anime.url)
            }
        }

        notifyChanges()
    }

    suspend fun removeSource(source: AnimeSource) {
        rootDownloadsDirMutex.withLock {
            rootDownloadsDir.sourceDirs -= source.id
        }

        notifyChanges()
    }

    fun invalidateCache() {
        lastRenew = 0L
        renewalJob?.cancel()
        diskCacheFile.delete()
        localEpisodeCountCacheFile.delete()
        renewCache()
    }

    /**
     * Renews the downloads cache.
     */
    private fun renewCache() {
        // Avoid renewing cache if in the process nor too often
        if (lastRenew + renewInterval >= System.currentTimeMillis() || renewalJob?.isActive == true) {
            return
        }

        renewalJob = scope.launchIO {
            if (lastRenew == 0L) {
                _isInitializing.emit(true)
            }
            try {
                joinAll(
                    launch { renewLocalEpisodeCount() },
                    launch { renewDownloadsCache() },
                )
            } finally {
                _isInitializing.emit(false)
            }
        }.apply {
            invokeOnCompletion(onCancelling = true) { exception ->
                if (exception != null && exception !is CancellationException) {
                    logcat(LogPriority.ERROR, exception) { "DownloadCache: failed to create cache" }
                }
                lastRenew = System.currentTimeMillis()
                notifyChanges()
            }
        }

        // Mainly to notify the indexing notifier UI
        notifyChanges()
    }

    private suspend fun renewDownloadsCache() = withIOContext {
        // Try to wait until extensions and sources have loaded
        var sources = emptyList<AnimeSource>()
        withTimeoutOrNull(30.seconds) {
            extensionManager.isInitialized.first { it }
            sourceManager.isInitialized.first { it }

            sources = getSources()
        }

        val sourceMap = sources.associate {
            provider.getSourceDirName(it).lowercase() to it.id
        }

        rootDownloadsDirMutex.withLock {
            val updatedRootDir = RootDirectory(storageManager.getDownloadsDirectory())

            updatedRootDir.sourceDirs = updatedRootDir.dir?.listFiles().orEmpty()
                .filter { it.isDirectory && !it.name.isNullOrBlank() }
                .mapNotNull { dir ->
                    val sourceId = sourceMap[dir.name!!.lowercase()]
                    sourceId?.let { it to SourceDirectory(dir) }
                }
                .toMap()

            updatedRootDir.sourceDirs.values.map { sourceDir ->
                async {
                    sourceDir.animeDirs = sourceDir.dir?.listFiles().orEmpty()
                        .filter { it.isDirectory && !it.name.isNullOrBlank() }
                        .associate { it.name!! to AnimeDirectory(it) }
                    sourceDir.animeDirs.values.forEach { animeDir ->
                        val episodeDirs = animeDir.dir?.listFiles().orEmpty()
                            .mapNotNull {
                                when {
                                    // Ignore incomplete downloads
                                    it.name?.endsWith(AnimeDownloader.TMP_DIR_SUFFIX) == true -> null
                                    // Folder of videos
                                    it.isDirectory -> it.name
                                    // MP4 and MKV files
                                    it.isFile && it.extension in setOf("mp4", "mkv") -> it.nameWithoutExtension
                                    // Anything else is irrelevant
                                    else -> null
                                }
                            }
                            .toMutableSet()

                        animeDir.episodeDirs = episodeDirs
                    }
                }
            }
                .awaitAll()

            rootDownloadsDir = updatedRootDir
        }
    }

    private suspend fun renewLocalEpisodeCount() = withIOContext {
        // if the cache has not been cleared via [SettingsAdvancedScreen]
        if (localEpisodeCountCacheFile.exists()) return@withIOContext

        val episodeCountsByAnime = rootLocalDir.getFilesInBaseDirectory()
            .filter { it.isDirectory }
            .map { animeDir ->
                async {
                    val count = animeDir.listFiles()
                        ?.map { async { it.isDirectory || ArchiveAnime.isSupported(it) } }
                        ?.awaitAll()
                        ?.count { it }
                        ?: 0

                    animeDir.name!! to EpisodeCount(
                        count,
                        animeDir.lastModified(),
                    )
                }
            }
            .awaitAll()
            .toMap()

        rootLocalDirMutex.withLock {
            localEpisodeCountCache += episodeCountsByAnime
        }
    }

    private fun getSources(): List<AnimeSource> {
        return sourceManager.getAll() + sourceManager.getStubSources()
    }

    private fun notifyChanges() {
        scope.launchNonCancellable {
            _changes.send(Unit)
        }
        updateDiskCache()
    }

    private var updateDiskCacheJob: Job? = null

    private fun updateDiskCache() {
        updateDiskCacheJob?.cancel()
        updateDiskCacheJob = scope.launchIO {
            delay(1000)
            ensureActive()
            rootDownloadsDirMutex.withLock { saveRootDownloadsCacheFile() }
            rootLocalDirMutex.withLock { saveLocalEpisodeCountCacheFile() }
        }
    }

    private fun saveRootDownloadsCacheFile() {
        val bytes = ProtoBuf.encodeToByteArray(rootDownloadsDir)
        try {
            diskCacheFile.writeBytes(bytes)
        } catch (e: Throwable) {
            logcat(
                priority = LogPriority.ERROR,
                throwable = e,
                message = { "Failed to write disk cache file" },
            )
        }
    }

    fun saveLocalEpisodeCountCacheFile() {
        val bytes = ProtoBuf.encodeToByteArray(localEpisodeCountCache)
        try {
            localEpisodeCountCacheFile.writeBytes(bytes)
        } catch (e: Throwable) {
            logcat(
                priority = LogPriority.ERROR,
                throwable = e,
                message = { "Failed to write disk cache file" },
            )
        }
    }

    /**
     * Force synchronization after loading all anime
     */
    fun sync() {
        updateDiskCache()
    }
}

/**
 * Class to store the files under the root downloads directory.
 */
@Serializable
private class RootDirectory(
    @Serializable(with = UniFileAsStringSerializer::class)
    val dir: UniFile?,
    var sourceDirs: Map<Long, SourceDirectory> = mapOf(),
)

/**
 * Class to store the files under a source directory.
 */
@Serializable
private class SourceDirectory(
    @Serializable(with = UniFileAsStringSerializer::class)
    val dir: UniFile?,
    var animeDirs: Map<String, AnimeDirectory> = mapOf(),
)

/**
 * Class to store the files under a anime directory.
 */
@Serializable
private class AnimeDirectory(
    @Serializable(with = UniFileAsStringSerializer::class)
    val dir: UniFile?,
    var episodeDirs: MutableSet<String> = mutableSetOf(),
)

private object UniFileAsStringSerializer : KSerializer<UniFile?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UniFile", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UniFile?) {
        return if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value.uri.toString())
        }
    }

    override fun deserialize(decoder: Decoder): UniFile? {
        return if (decoder.decodeNotNullMark()) {
            UniFile.fromUri(Injekt.get<Application>(), decoder.decodeString().toUri())
        } else {
            decoder.decodeNull()
        }
    }
}

@Serializable
private class EpisodeCount(
    var count: Int,
    val lastModified: Long,
)
