package tachiyomi.source.local.entries.anime

import android.content.Context
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.UnmeteredSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.toFFmpegString
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import logcat.LogPriority
import rx.Observable
import tachiyomi.core.metadata.tachiyomi.AnimeDetails
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.service.EpisodeRecognition
import tachiyomi.source.local.R
import tachiyomi.source.local.filter.anime.AnimeOrderBy
import tachiyomi.source.local.image.anime.LocalAnimeCoverManager
import tachiyomi.source.local.io.ArchiveAnime
import tachiyomi.source.local.io.anime.LocalAnimeSourceFileSystem
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.TimeUnit

actual class LocalAnimeSource(
    private val context: Context,
    private val fileSystem: LocalAnimeSourceFileSystem,
    private val coverManager: LocalAnimeCoverManager,
) : AnimeCatalogueSource, UnmeteredSource {

    private val json: Json by injectLazy()

    private val POPULAR_FILTERS = AnimeFilterList(AnimeOrderBy.Popular(context))
    private val LATEST_FILTERS = AnimeFilterList(AnimeOrderBy.Latest(context))

    override val name = context.getString(R.string.local_anime_source)

    override val id: Long = ID

    override val lang = "other"

    override fun toString() = name

    override val supportsLatest = true

    // Browse related
    override fun fetchPopularAnime(page: Int) = fetchSearchAnime(page, "", POPULAR_FILTERS)

    override fun fetchLatestUpdates(page: Int) = fetchSearchAnime(page, "", LATEST_FILTERS)

    override fun fetchSearchAnime(page: Int, query: String, filters: AnimeFilterList): Observable<AnimesPage> {
        val baseDirsFiles = fileSystem.getFilesInBaseDirectories()
        val lastModifiedLimit by lazy { if (filters === LATEST_FILTERS) System.currentTimeMillis() - LATEST_THRESHOLD else 0L }

        var animeDirs = baseDirsFiles
            // Filter out files that are hidden and is not a folder
            .filter { it.isDirectory && !it.name.startsWith('.') }
            .distinctBy { it.name }
            .filter { // Filter by query or last modified
                if (lastModifiedLimit == 0L) {
                    it.name.contains(query, ignoreCase = true)
                } else {
                    it.lastModified() >= lastModifiedLimit
                }
            }

        filters.forEach { filter ->
            when (filter) {
                is AnimeOrderBy.Popular -> {
                    animeDirs = if (filter.state!!.ascending) {
                        animeDirs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                    } else {
                        animeDirs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
                    }
                }
                is AnimeOrderBy.Latest -> {
                    animeDirs = if (filter.state!!.ascending) {
                        animeDirs.sortedBy(File::lastModified)
                    } else {
                        animeDirs.sortedByDescending(File::lastModified)
                    }
                }

                else -> {
                    /* Do nothing */
                }
            }
        }

        // Transform animeDirs to list of SAnime
        val animes = animeDirs.map { animeDir ->
            SAnime.create().apply {
                title = animeDir.name
                url = animeDir.name

                // Try to find the cover
                coverManager.find(animeDir.name)
                    ?.takeIf(File::exists)
                    ?.let { thumbnail_url = it.absolutePath }
            }
        }

        // Fetch episodes of all the anime
        animes.forEach { anime ->
            runBlocking {
                val episodes = getEpisodeList(anime)
                if (episodes.isNotEmpty()) {
                    val episode = episodes.last()
                    // Copy the cover from the first episode found if not available
                    if (anime.thumbnail_url == null) {
                        try {
                            updateCoverFromVideo(episode, anime)
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR) { "Couldn't extract thumbnail from video." }
                        }
                    }
                }
            }
        }

        return Observable.just(AnimesPage(animes.toList(), false))
    }

    // Anime details related
    override suspend fun getAnimeDetails(anime: SAnime): SAnime = withIOContext {
        coverManager.find(anime.url)?.let {
            anime.thumbnail_url = it.absolutePath
        }

        val animeDirFiles = fileSystem.getFilesInAnimeDirectory(anime.url).toList()

        animeDirFiles
            .firstOrNull { it.extension == "json" }
            ?.let { file ->
                json.decodeFromStream<AnimeDetails>(file.inputStream()).run {
                    title?.let { anime.title = it }
                    author?.let { anime.author = it }
                    artist?.let { anime.artist = it }
                    description?.let { anime.description = it }
                    genre?.let { anime.genre = it.joinToString() }
                    status?.let { anime.status = it }
                }
            }

        return@withIOContext anime
    }

    // Episodes
    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        return fileSystem.getFilesInAnimeDirectory(anime.url)
            // Only keep supported formats
            .filter { it.isDirectory || ArchiveAnime.isSupported(it) }
            .map { episodeFile ->
                SEpisode.create().apply {
                    url = episodeFile.absolutePath
                    name = if (episodeFile.isDirectory) {
                        episodeFile.name
                    } else {
                        episodeFile.nameWithoutExtension
                    }
                    date_upload = episodeFile.lastModified()

                    episode_number = EpisodeRecognition.parseEpisodeNumber(
                        anime.title,
                        this.name,
                        this.episode_number,
                    )
                }
            }
            .sortedWith { e1, e2 ->
                val e = e2.episode_number.compareTo(e1.episode_number)
                if (e == 0) e2.name.compareToCaseInsensitiveNaturalOrder(e1.name) else e
            }
            .toList()
    }

    // Filters
    override fun getFilterList() = AnimeFilterList(AnimeOrderBy.Popular(context))

    // Unused stuff
    override suspend fun getVideoList(episode: SEpisode) = throw UnsupportedOperationException("Unused")

    private fun updateCoverFromVideo(episode: SEpisode, anime: SAnime) {
        val baseDirsFiles = getBaseDirectoriesFiles(context)
        val animeDir = getAnimeDir(anime.url, baseDirsFiles) ?: return
        val coverPath = "${animeDir.absolutePath}/$DEFAULT_COVER_NAME"

        val episodeFilename = { episode.url.toFFmpegString(context) }
        val ffProbe = com.arthenica.ffmpegkit.FFprobeKit.execute(
            "-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 \"${episodeFilename()}\"",
        )
        val duration = ffProbe.allLogsAsString.trim().toFloat()
        val second = duration.toInt() / 2

        com.arthenica.ffmpegkit.FFmpegKit.execute("-ss $second -i \"${episodeFilename()}\" -frames:v 1 -update true \"$coverPath\" -y")

        if (File(coverPath).exists()) {
            anime.thumbnail_url = coverPath
        }
    }

    companion object {
        const val ID = 0L
        const val HELP_URL = "https://aniyomi.org/help/guides/local-anime/"

        private const val DEFAULT_COVER_NAME = "cover.jpg"
        private val LATEST_THRESHOLD = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS)

        private fun getBaseDirectories(context: Context): Sequence<File> {
            val localFolder = context.getString(R.string.app_name) + File.separator + "localanime"
            return DiskUtil.getExternalStorages(context)
                .map { File(it.absolutePath, localFolder) }
                .asSequence()
        }

        private fun getBaseDirectoriesFiles(context: Context): Sequence<File> {
            return getBaseDirectories(context)
                // Get all the files inside all baseDir
                .flatMap { it.listFiles().orEmpty().toList() }
        }

        private fun getAnimeDir(animeUrl: String, baseDirsFile: Sequence<File>): File? {
            return baseDirsFile
                // Get the first animeDir or null
                .firstOrNull { it.isDirectory && it.name == animeUrl }
        }
    }
}

fun Anime.isLocal(): Boolean = source == LocalAnimeSource.ID

fun AnimeSource.isLocal(): Boolean = id == LocalAnimeSource.ID
