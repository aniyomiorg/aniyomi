package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

/**
 * This class is used to provide the directories where the downloads should be saved.
 * It uses the following path scheme: /<root downloads dir>/<source name>/<anime>/<episode>
 *
 * @param context the application context.
 */
class AnimeDownloadProvider(private val context: Context) {

    private val preferences: PreferencesHelper by injectLazy()

    private val scope = MainScope()

    /**
     * The root directory for downloads.
     */
    private var downloadsDir = preferences.downloadsDirectory().get().let {
        val dir = UniFile.fromUri(context, it.toUri())
        DiskUtil.createNoMediaFile(dir, context)
        dir
    }

    init {
        preferences.downloadsDirectory().asFlow()
            .onEach { downloadsDir = UniFile.fromUri(context, it.toUri()) }
            .launchIn(scope)
    }

    /**
     * Returns the download directory for a anime. For internal use only.
     *
     * @param anime the anime to query.
     * @param source the source of the anime.
     */
    internal fun getAnimeDir(anime: Anime, source: AnimeSource): UniFile {
        try {
            return downloadsDir
                .createDirectory(getSourceDirName(source))
                .createDirectory(getAnimeDirName(anime))
        } catch (e: Throwable) {
            Timber.e(e, "Invalid download directory")
            throw Exception(context.getString(R.string.invalid_download_dir))
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
     * Returns the download directory for a anime if it exists.
     *
     * @param anime the anime to query.
     * @param source the source of the anime.
     */
    fun findAnimeDir(anime: Anime, source: AnimeSource): UniFile? {
        val sourceDir = findSourceDir(source)
        return sourceDir?.findFile(getAnimeDirName(anime), true)
    }

    /**
     * Returns the download directory for a episode if it exists.
     *
     * @param episode the episode to query.
     * @param anime the anime of the episode.
     * @param source the source of the episode.
     */
    fun findEpisodeDir(episode: Episode, anime: Anime, source: AnimeSource): UniFile? {
        val animeDir = findAnimeDir(anime, source)
        return getValidEpisodeDirNames(episode).asSequence()
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
    fun findEpisodeDirs(episodes: List<Episode>, anime: Anime, source: AnimeSource): List<UniFile> {
        val animeDir = findAnimeDir(anime, source) ?: return emptyList()
        return episodes.mapNotNull { episode ->
            getValidEpisodeDirNames(episode).asSequence()
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
     * Returns the download directory name for a anime.
     *
     * @param anime the anime to query.
     */
    fun getAnimeDirName(anime: Anime): String {
        return DiskUtil.buildValidFilename(anime.title)
    }

    /**
     * Returns the episode directory name for a episode.
     *
     * @param episode the episode to query.
     */
    fun getEpisodeDirName(episode: Episode): String {
        return DiskUtil.buildValidFilename(
            when {
                episode.scanlator != null -> "${episode.scanlator}_${episode.name}"
                else -> episode.name
            }
        )
    }

    /**
     * Returns valid downloaded episode directory names.
     *
     * @param episode the episode to query.
     */
    fun getValidEpisodeDirNames(episode: Episode): List<String> {
        return listOf(
            getEpisodeDirName(episode),
            // TODO: remove this
            // Legacy episode directory name used in v0.9.2 and before
            DiskUtil.buildValidFilename(episode.name)
        )
    }
}
