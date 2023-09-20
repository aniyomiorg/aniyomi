package eu.kanade.tachiyomi.data.download.anime

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.core.util.mapNotNullKeys
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap
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
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow()
        .onStart { emit(Unit) }
        .shareIn(scope, SharingStarted.Eagerly, 1)

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

    private var rootDownloadsDir = RootDirectory(getDirectoryFromPreference())

    init {
        downloadPreferences.downloadsDirectory().changes()
            .onEach {
                rootDownloadsDir = RootDirectory(getDirectoryFromPreference())
                invalidateCache()
            }
            .launchIn(scope)
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
                return provider.getValidEpisodeDirNames(episodeName, episodeScanlator).any { it in animeDir.episodeDirs }
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

    /**
     * Adds an episode that has just been download to this cache.
     *
     * @param episodeDirName the downloaded episode's directory name.
     * @param animeUniFile the directory of the anime.
     * @param anime the anime of the episode.
     */
    @Synchronized
    fun addEpisode(episodeDirName: String, animeUniFile: UniFile, anime: Anime) {
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

        // Save the episode directory
        animeDir.episodeDirs += episodeDirName

        notifyChanges()
    }

    /**
     * Removes an episode that has been deleted from this cache.
     *
     * @param episode the episode to remove.
     * @param anime the anime of the episode.
     */
    @Synchronized
    fun removeEpisode(episode: Episode, anime: Anime) {
        val sourceDir = rootDownloadsDir.sourceDirs[anime.source] ?: return
        val animeDir = sourceDir.animeDirs[provider.getAnimeDirName(anime.title)] ?: return
        provider.getValidEpisodeDirNames(episode.name, episode.scanlator).forEach {
            if (it in animeDir.episodeDirs) {
                animeDir.episodeDirs -= it
            }
        }
    }

    /**
     * Removes a list of episodes that have been deleted from this cache.
     *
     * @param episodes the list of episode to remove.
     * @param anime the anime of the episode.
     */
    @Synchronized
    fun removeEpisodes(episodes: List<Episode>, anime: Anime) {
        val sourceDir = rootDownloadsDir.sourceDirs[anime.source] ?: return
        val animeDir = sourceDir.animeDirs[provider.getAnimeDirName(anime.title)] ?: return
        episodes.forEach { episode ->
            provider.getValidEpisodeDirNames(episode.name, episode.scanlator).forEach {
                if (it in animeDir.episodeDirs) {
                    animeDir.episodeDirs -= it
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
    @Synchronized
    fun removeAnime(anime: Anime) {
        val sourceDir = rootDownloadsDir.sourceDirs[anime.source] ?: return
        val animeDirName = provider.getAnimeDirName(anime.title)
        if (sourceDir.animeDirs.containsKey(animeDirName)) {
            sourceDir.animeDirs -= animeDirName
        }

        notifyChanges()
    }

    fun removeSource(source: AnimeSource) {
        rootDownloadsDir.sourceDirs -= source.id

        notifyChanges()
    }

    fun invalidateCache() {
        lastRenew = 0L
        renewalJob?.cancel()
    }

    /**
     * Returns the downloads directory from the user's preferences.
     */
    private fun getDirectoryFromPreference(): UniFile {
        val dir = downloadPreferences.downloadsDirectory().get()
        return UniFile.fromUri(context, dir.toUri())
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

            var sources = getSources()

            // Try to wait until extensions and sources have loaded
            withTimeoutOrNull(30.seconds) {
                while (!extensionManager.isInitialized) {
                    delay(2.seconds)
                }

                while (sources.isEmpty()) {
                    delay(2.seconds)
                    sources = getSources()
                }
            }

            val sourceDirs = rootDownloadsDir.dir.listFiles().orEmpty()
                .associate { it.name to SourceDirectory(it) }
                .mapNotNullKeys { entry ->
                    sources.find {
                        provider.getSourceDirName(it).equals(entry.key, ignoreCase = true)
                    }?.id
                }

            rootDownloadsDir.sourceDirs = sourceDirs

            sourceDirs.values
                .map { sourceDir ->
                    async {
                        val animeDirs = sourceDir.dir.listFiles().orEmpty()
                            .filterNot { it.name.isNullOrBlank() }
                            .associate { it.name!! to AnimeDirectory(it) }

                        sourceDir.animeDirs = ConcurrentHashMap(animeDirs)

                        animeDirs.values.forEach { animeDir ->
                            val episodeDirs = animeDir.dir.listFiles().orEmpty()
                                .mapNotNull {
                                    when {
                                        // Ignore incomplete downloads
                                        it.name?.endsWith(AnimeDownloader.TMP_DIR_SUFFIX) == true -> null
                                        // Folder of images
                                        it.isDirectory -> it.name
                                        // MP4 files
                                        it.isFile && it.name?.endsWith(".mp4") == true -> it.name!!.substringBeforeLast(".mp4")
                                        // MKV files
                                        it.isFile && it.name?.endsWith(".mkv") == true -> it.name!!.substringBeforeLast(".mkv")
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

            _isInitializing.emit(false)
        }.also {
            it.invokeOnCompletion(onCancelling = true) { exception ->
                if (exception != null && exception !is CancellationException) {
                    logcat(LogPriority.ERROR, exception) { "Failed to create download cache" }
                }

                lastRenew = System.currentTimeMillis()
                notifyChanges()
            }
        }

        // Mainly to notify the indexing notifier UI
        notifyChanges()
    }

    private fun getSources(): List<AnimeSource> {
        return sourceManager.getOnlineSources() + sourceManager.getStubSources()
    }

    private fun notifyChanges() {
        scope.launchNonCancellable {
            _changes.send(Unit)
        }
    }
}

/**
 * Class to store the files under the root downloads directory.
 */
private class RootDirectory(
    val dir: UniFile,
    var sourceDirs: ConcurrentHashMap<Long, SourceDirectory> = ConcurrentHashMap(),
)

/**
 * Class to store the files under a source directory.
 */
private class SourceDirectory(
    val dir: UniFile,
    var animeDirs: ConcurrentHashMap<String, AnimeDirectory> = ConcurrentHashMap(),
)

/**
 * Class to store the files under a manga directory.
 */
private class AnimeDirectory(
    val dir: UniFile,
    var episodeDirs: MutableSet<String> = mutableSetOf(),
)
