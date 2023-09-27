package eu.kanade.tachiyomi.data.download.anime

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This class is used to provide the directories where the downloads should be saved.
 * It uses the following path scheme: /<root downloads dir>/<source name>/<anime>/<episode>
 *
 * @param context the application context.
 */
class AnimeDownloadProvider(
    private val context: Context,
    downloadPreferences: DownloadPreferences = Injekt.get(),
) {

    private val scope = MainScope()

    /**
     * The root directory for downloads.
     */
    private var downloadsDir = downloadPreferences.downloadsDirectory().get().let {
        val dir = UniFile.fromUri(context, it.toUri())
        DiskUtil.createNoMediaFile(dir, context)
        dir
    }

    init {
        downloadPreferences.downloadsDirectory().changes()
            .onEach { downloadsDir = UniFile.fromUri(context, it.toUri()) }
            .launchIn(scope)
    }

    /**
     * Returns the download directory for an anime. For internal use only.
     *
     * @param animeTitle the title of the anime to query.
     * @param source the source of the anime.
     */
    internal fun getAnimeDir(animeTitle: String, source: AnimeSource): UniFile {
        try {
            return downloadsDir
                .createDirectory(getSourceDirName(source))
                .createDirectory(getAnimeDirName(animeTitle))
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Invalid download directory" }
            throw Exception(context.getString(R.string.invalid_location, downloadsDir))
        }
    }

    /**
     * Returns the download directory for a source if it exists.
     *
     * @param source the source to query.
     */
    fun findSourceDir(source: AnimeSource): UniFile? {
        return downloadsDir.findFile(getSourceDirName(source), true)
    }

    /**
     * Returns the download directory for an anime if it exists.
     *
     * @param animeTitle the title of the anime to query.
     * @param source the source of the anime.
     */
    fun findAnimeDir(animeTitle: String, source: AnimeSource): UniFile? {
        val sourceDir = findSourceDir(source)
        return sourceDir?.findFile(getAnimeDirName(animeTitle), true)
    }

    /**
     * Returns the download directory for an episode if it exists.
     *
     * @param episodeName the name of the episode to query.
     * @param episodeScanlator scanlator of the episode to query
     * @param animeTitle the title of the anime to query.
     * @param source the source of the episode.
     */
    fun findEpisodeDir(episodeName: String, episodeScanlator: String?, animeTitle: String, source: AnimeSource): UniFile? {
        val animeDir = findAnimeDir(animeTitle, source)
        return getValidEpisodeDirNames(episodeName, episodeScanlator).asSequence()
            .mapNotNull { animeDir?.findFile(it, true) }
            .firstOrNull()
    }

    /**
     * Returns a list of downloaded directories for the episodes that exist.
     *
     * @param episodes the episodes to query.
     * @param anime the anime of the episode.
     * @param source the source of the episode.
     */
    fun findEpisodeDirs(episodes: List<Episode>, anime: Anime, source: AnimeSource): Pair<UniFile?, List<UniFile>> {
        val animeDir = findAnimeDir(anime.title, source) ?: return null to emptyList()
        return animeDir to episodes.mapNotNull { episode ->
            getValidEpisodeDirNames(episode.name, episode.scanlator).asSequence()
                .mapNotNull { animeDir.findFile(it) }
                .firstOrNull()
        }
    }

    /**
     * Returns the download directory name for a source.
     *
     * @param source the source to query.
     */
    fun getSourceDirName(source: AnimeSource): String {
        return DiskUtil.buildValidFilename(source.toString())
    }

    /**
     * Returns the download directory name for an anime.
     *
     * @param animeTitle the title of the anime to query.
     */
    fun getAnimeDirName(animeTitle: String): String {
        return DiskUtil.buildValidFilename(animeTitle)
    }

    /**
     * Returns the episode directory name for an episode.
     *
     * @param episodeName the name of the episode to query.
     * @param episodeScanlator scanlator of the episode to query
     */
    fun getEpisodeDirName(episodeName: String, episodeScanlator: String?): String {
        val newEpisodeName = sanitizeEpisodeName(episodeName)
        return DiskUtil.buildValidFilename(
            when {
                episodeScanlator.isNullOrBlank().not() -> "${episodeScanlator}_$newEpisodeName"
                else -> newEpisodeName
            },
        )
    }

    /**
     * Return the new name for the episode (in case it's empty or blank)
     *
     * @param episodeName the name of the episode
     */
    private fun sanitizeEpisodeName(episodeName: String): String {
        return episodeName.ifBlank {
            "Episode"
        }
    }

    /**
     * Returns the episode directory name for an episode.
     *
     * @param episodeName the name of the episode to query.
     * @param episodeScanlator scanlator of the episode to query
     */
    fun getOldEpisodeDirName(episodeName: String, episodeScanlator: String?): String {
        return DiskUtil.buildValidFilename(
            when {
                episodeScanlator != null -> "${episodeScanlator}_$episodeName"
                else -> episodeName
            },
        )
    }

    fun isEpisodeDirNameChanged(oldEpisode: Episode, newEpisode: Episode): Boolean {
        return oldEpisode.name != newEpisode.name ||
            oldEpisode.scanlator?.takeIf { it.isNotBlank() } != newEpisode.scanlator?.takeIf { it.isNotBlank() }
    }

    /**
     * Returns valid downloaded episode directory names.
     *
     * @param episodeName the name of the episode to query.
     * @param episodeScanlator scanlator of the episode to query
     */
    fun getValidEpisodeDirNames(episodeName: String, episodeScanlator: String?): List<String> {
        val episodeDirName = getEpisodeDirName(episodeName, episodeScanlator)
        val oldEpisodeDirName = getOldEpisodeDirName(episodeName, episodeScanlator)
        return listOf(episodeDirName, oldEpisodeDirName)
    }
}
