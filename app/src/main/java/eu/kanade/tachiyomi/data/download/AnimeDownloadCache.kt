package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.episode.model.Episode
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

/**
 * Cache where we dump the downloads directory from the filesystem. This class is needed because
 * directory checking is expensive and it slowdowns the app. The cache is invalidated by the time
 * defined in [renewInterval] as we don't have any control over the filesystem and the user can
 * delete the folders at any time without the app noticing.
 *
 * @param context the application context.
 * @param provider the downloads directories provider.
 * @param sourceManager the source manager.
 * @param preferences the preferences of the app.
 */
class AnimeDownloadCache(
    private val context: Context,
    private val provider: AnimeDownloadProvider,
    private val sourceManager: AnimeSourceManager,
    private val preferences: PreferencesHelper = Injekt.get(),
) {

    /**
     * The interval after which this cache should be invalidated. 1 hour shouldn't cause major
     * issues, as the cache is only used for UI feedback.
     */
    private val renewInterval = TimeUnit.HOURS.toMillis(1)

    /**
     * The last time the cache was refreshed.
     */
    private var lastRenew = 0L

    /**
     * The root directory for downloads.
     */
    private var rootDir = RootDirectory(getDirectoryFromPreference())

    init {
        preferences.downloadsDirectory().asFlow()
            .onEach {
                lastRenew = 0L // invalidate cache
                rootDir = RootDirectory(getDirectoryFromPreference())
            }
    }

    /**
     * Returns the downloads directory from the user's preferences.
     */
    private fun getDirectoryFromPreference(): UniFile {
        val dir = preferences.downloadsDirectory().get()
        return UniFile.fromUri(context, dir.toUri())
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
     * Returns the amount of downloaded episodes for a anime.
     *
     * @param anime the anime to check.
     */
    fun getDownloadCount(anime: Anime): Int {
        checkRenew()

        val sourceDir = rootDir.files[anime.source]
        if (sourceDir != null) {
            val animeDir = sourceDir.files[provider.getAnimeDirName(anime.title)]
            if (animeDir != null) {
                return animeDir.files
                    .filter { !it.endsWith(AnimeDownloader.TMP_DIR_SUFFIX) }
                    .size
            }
        }
        return 0
    }

    /**
     * Checks if the cache needs a renewal and performs it if needed.
     */
    @Synchronized
    private fun checkRenew() {
        if (lastRenew + renewInterval < System.currentTimeMillis()) {
            renew()
            lastRenew = System.currentTimeMillis()
        }
    }

    /**
     * Renews the downloads cache.
     */
    private fun renew() {
        val onlineSources = sourceManager.getOnlineSources()

        val stubSources = sourceManager.getStubSources()

        val allSource = onlineSources + stubSources

        val sourceDirs = rootDir.dir.listFiles()
            .orEmpty()
            .associate { it.name to SourceDirectory(it) }
            .mapNotNullKeys { entry ->
                allSource.find { provider.getSourceDirName(it).equals(entry.key, ignoreCase = true) }?.id
            }

        rootDir.files = sourceDirs

        sourceDirs.values.forEach { sourceDir ->
            val animeDirs = sourceDir.dir.listFiles()
                .orEmpty()
                .associateNotNullKeys { it.name to AnimeDirectory(it) }

            sourceDir.files = animeDirs

            animeDirs.values.forEach { animeDir ->
                val episodeDirs = animeDir.dir.listFiles()
                    .orEmpty()
                    .mapNotNull { it.name }
                    .toHashSet()

                animeDir.files = episodeDirs
            }
        }
    }

    /**
     * Adds a episode that has just been download to this cache.
     *
     * @param episodeDirName the downloaded episode's directory name.
     * @param animeUniFile the directory of the anime.
     * @param anime the anime of the episode.
     */
    @Synchronized
    fun addEpisode(episodeDirName: String, animeUniFile: UniFile, anime: Anime) {
        // Retrieve the cached source directory or cache a new one
        var sourceDir = rootDir.files[anime.source]
        if (sourceDir == null) {
            val source = sourceManager.get(anime.source) ?: return
            val sourceUniFile = provider.findSourceDir(source) ?: return
            sourceDir = SourceDirectory(sourceUniFile)
            rootDir.files += anime.source to sourceDir
        }

        // Retrieve the cached anime directory or cache a new one
        val animeDirName = provider.getAnimeDirName(anime.title)
        var animeDir = sourceDir.files[animeDirName]
        if (animeDir == null) {
            animeDir = AnimeDirectory(animeUniFile)
            sourceDir.files += animeDirName to animeDir
        }

        // Save the episode directory
        animeDir.files += episodeDirName
    }

    /**
     * Removes a episode that has been deleted from this cache.
     *
     * @param episode the episode to remove.
     * @param anime the anime of the episode.
     */
    @Synchronized
    fun removeEpisode(episode: Episode, anime: Anime) {
        val sourceDir = rootDir.files[anime.source] ?: return
        val animeDir = sourceDir.files[provider.getAnimeDirName(anime.title)] ?: return
        provider.getValidEpisodeDirNames(episode.name, episode.scanlator).forEach {
            if (it in animeDir.files) {
                animeDir.files -= it
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
        val sourceDir = rootDir.files[anime.source] ?: return
        val animeDir = sourceDir.files[provider.getAnimeDirName(anime.title)] ?: return
        episodes.forEach { episode ->
            provider.getValidEpisodeDirNames(episode.name, episode.scanlator).forEach {
                if (it in animeDir.files) {
                    animeDir.files -= it
                }
            }
        }
    }

    /**
     * Removes a anime that has been deleted from this cache.
     *
     * @param anime the anime to remove.
     */
    @Synchronized
    fun removeAnime(anime: Anime) {
        val sourceDir = rootDir.files[anime.source] ?: return
        val animeDirName = provider.getAnimeDirName(anime.title)
        if (animeDirName in sourceDir.files) {
            sourceDir.files -= animeDirName
        }
    }

    /**
     * Class to store the files under the root downloads directory.
     */
    private class RootDirectory(
        val dir: UniFile,
        var files: Map<Long, SourceDirectory> = hashMapOf(),
    )

    /**
     * Class to store the files under a source directory.
     */
    private class SourceDirectory(
        val dir: UniFile,
        var files: Map<String, AnimeDirectory> = hashMapOf(),
    )

    /**
     * Class to store the files under a anime directory.
     */
    private class AnimeDirectory(
        val dir: UniFile,
        var files: Set<String> = hashSetOf(),
    )

    /**
     * Returns a new map containing only the key entries of [transform] that are not null.
     */
    private inline fun <K, V, R> Map<out K, V>.mapNotNullKeys(transform: (Map.Entry<K?, V>) -> R?): Map<R, V> {
        val destination = LinkedHashMap<R, V>()
        forEach { element -> transform(element)?.let { destination[it] = element.value } }
        return destination
    }

    /**
     * Returns a map from a list containing only the key entries of [transform] that are not null.
     */
    private inline fun <T, K, V> Array<T>.associateNotNullKeys(transform: (T) -> Pair<K?, V>): Map<K, V> {
        val destination = LinkedHashMap<K, V>()
        for (element in this) {
            val (key, value) = transform(element)
            if (key != null) {
                destination[key] = value
            }
        }
        return destination
    }
}
