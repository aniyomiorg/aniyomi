package eu.kanade.tachiyomi.data.animedownload

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.download.service.DownloadPreferences
import eu.kanade.tachiyomi.animeextension.AnimeExtensionManager
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withTimeout
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
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

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow().onStart { emit(Unit) }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val notifier by lazy { AnimeDownloadNotifier(context) }

    /**
     * The interval after which this cache should be invalidated. 1 hour shouldn't cause major
     * issues, as the cache is only used for UI feedback.
     */
    private val renewInterval = TimeUnit.HOURS.toMillis(1)

    /**
     * The last time the cache was refreshed.
     */
    private var lastRenew = 0L
    private var renewalJob: Job? = null

    private var rootDownloadsDir = RootDirectory(getDirectoryFromPreference())

    init {
        downloadPreferences.downloadsDirectory().changes()
            .onEach {
                rootDownloadsDir = RootDirectory(getDirectoryFromPreference())
                // Invalidate cache
                lastRenew = 0L
            }
            .launchIn(scope)
    }

    /**
     * Returns true if the episode is downloaded.
     *
     * @param episodeName the name of the episode to query.
     * @param chapterScanlator scanlator of the chapter to query
     * @param animeTitle the title of the anime to query.
     * @param sourceId the id of the source of the episode.
     * @param skipCache whether to skip the directory cache and check in the filesystem.
     */
    fun isEpisodeDownloaded(
        episodeName: String,
        chapterScanlator: String?,
        animeTitle: String,
        sourceId: Long,
    ): Boolean {
        val source = sourceManager.getOrStub(sourceId)
        return provider.findEpisodeDir(episodeName, chapterScanlator, animeTitle, source) != null
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

    @Synchronized
    fun removeSourceIfEmpty(source: AnimeSource) {
        val sourceDir = provider.findSourceDir(source)
        if (sourceDir?.listFiles()?.isEmpty() == true) {
            sourceDir.delete()
            rootDownloadsDir.sourceDirs -= source.id
        }

        notifyChanges()
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
            try {
                notifier.onCacheProgress()
                var sources = getSources()

                // Try to wait until extensions and sources have loaded
                withTimeout(30.seconds) {
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
                                    .mapNotNull { episodeDir ->
                                        episodeDir.name
                                            ?.replace(".cbz", "")
                                            ?.takeUnless { it.endsWith(AnimeDownloader.TMP_DIR_SUFFIX) }
                                    }
                                    .toMutableSet()

                                animeDir.episodeDirs = episodeDirs
                            }
                        }
                    }
                    .awaitAll()

                lastRenew = System.currentTimeMillis()
                notifyChanges()
            } finally {
                notifier.dismissCacheProgress()
            }
        }
    }

    private fun getSources(): List<AnimeSource> {
        return sourceManager.getOnlineSources() + sourceManager.getStubSources()
    }

    private fun notifyChanges() {
        scope.launchNonCancellable {
            _changes.send(Unit)
        }
    }

    /**
     * Returns a new map containing only the key entries of [transform] that are not null.
     */
    private inline fun <K, V, R> Map<out K, V>.mapNotNullKeys(transform: (Map.Entry<K?, V>) -> R?): ConcurrentHashMap<R, V> {
        val mutableMap = ConcurrentHashMap<R, V>()
        forEach { element -> transform(element)?.let { mutableMap[it] = element.value } }
        return mutableMap
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
