package tachiyomi.source.local.entries.anime

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.UnmeteredSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimeRelation
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate
import eu.kanade.tachiyomi.animesource.model.SAnimeSeasonUpdate
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import eu.kanade.tachiyomi.util.storage.toFFmpegString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.core.metadata.tachiyomi.AnimeDetails
import tachiyomi.core.metadata.tachiyomi.EpisodeDetails
import tachiyomi.domain.entries.anime.interactor.GetAnimeByUrlAndSourceId
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUpdate
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.items.episode.interactor.GetEpisodeByUrlAndAnimeId
import tachiyomi.domain.items.episode.interactor.UpdateEpisode
import tachiyomi.domain.items.episode.model.EpisodeUpdate
import tachiyomi.domain.items.episode.service.EpisodeRecognition
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.source.local.filter.anime.AnimeOrderBy
import tachiyomi.source.local.image.anime.LocalAnimeBackgroundManager
import tachiyomi.source.local.image.anime.LocalAnimeCoverManager
import tachiyomi.source.local.image.anime.LocalEpisodeThumbnailManager
import tachiyomi.source.local.io.ArchiveAnime
import tachiyomi.source.local.io.anime.LocalAnimeSourceFileSystem
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

actual class LocalAnimeSource(
    private val context: Context,
    private val fileSystem: LocalAnimeSourceFileSystem,
    private val coverManager: LocalAnimeCoverManager,
    private val backgroundManager: LocalAnimeBackgroundManager,
    private val thumbnailManager: LocalEpisodeThumbnailManager,
    private val fetchTypeManager: LocalAnimeFetchTypeManager,
) : AnimeSource, UnmeteredSource {

    private val json: Json by injectLazy()
    private val getAnimeByUrlAndSourceId: GetAnimeByUrlAndSourceId by injectLazy()
    private val getEpisodeByUrlAndAnimeId: GetEpisodeByUrlAndAnimeId by injectLazy()
    private val updateEpisode: UpdateEpisode by injectLazy()
    private val animeRepository: AnimeRepository by injectLazy()
    private val libraryPreferences: LibraryPreferences by injectLazy()

    private val thumbnailScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val thumbnailSemaphore = Semaphore(2)

    @Suppress("PrivatePropertyName")
    private val PopularFilters = AnimeFilterList(AnimeOrderBy.Popular(context))

    @Suppress("PrivatePropertyName")
    private val LatestFilters = AnimeFilterList(AnimeOrderBy.Latest(context))

    override val name = context.stringResource(AYMR.strings.local_anime_source)

    override val id: Long = ID

    override val lang = "other"

    override fun toString() = name

    override val supportsLatest = true

    // Browse related
    override suspend fun getPopularAnime(page: Int) = getSearchAnime(page, "", PopularFilters)

    override suspend fun getLatestUpdates(page: Int) = getSearchAnime(page, "", LatestFilters)

    override suspend fun getSearchAnime(
        page: Int,
        query: String,
        filters: AnimeFilterList,
    ): AnimesPage = withIOContext {
        val lastModifiedLimit = if (filters === LatestFilters) {
            System.currentTimeMillis() - LATEST_THRESHOLD
        } else {
            0L
        }

        var animeDirs = fileSystem.getFilesInBaseDirectory()
            // Filter out files that are hidden and is not a folder
            .filter { it.isDirectory && !it.name.orEmpty().startsWith('.') }
            .distinctBy { it.name }
            .filter {
                if (lastModifiedLimit == 0L && query.isBlank()) {
                    true
                } else if (lastModifiedLimit == 0L) {
                    it.name.orEmpty().contains(query, ignoreCase = true)
                } else {
                    it.lastModified() >= lastModifiedLimit
                }
            }

        filters.forEach { filter ->
            when (filter) {
                is AnimeOrderBy.Popular -> {
                    animeDirs = if (filter.state!!.ascending) {
                        animeDirs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() })
                    } else {
                        animeDirs.sortedWith(
                            compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() },
                        )
                    }
                }
                is AnimeOrderBy.Latest -> {
                    animeDirs = if (filter.state!!.ascending) {
                        animeDirs.sortedBy(UniFile::lastModified)
                    } else {
                        animeDirs.sortedByDescending(UniFile::lastModified)
                    }
                }
                else -> {
                    /* Do nothing */
                }
            }
        }

        // Transform animeDirs to list of SAnime
        val animes = animeDirs
            .map { animeDir ->
                async {
                    getSAnime(animeDir.name)
                }
            }
            .awaitAll()

        AnimesPage(animes.toList(), false)
    }

    private fun getSAnime(animeDir: String?): SAnime {
        return SAnime.create().apply {
            title = animeDir.orEmpty().substringAfterLast(File.separator)
            url = animeDir.orEmpty()
            fetch_type = fetchTypeManager.find(animeDir.orEmpty())

            // Try to find the cover
            coverManager.find(animeDir.orEmpty())?.let {
                thumbnail_url = it.uri.toString()
            }

            // Try to find the background
            backgroundManager.find(animeDir.orEmpty())?.let {
                background_url = it.uri.toString()
            }
        }
    }

    override suspend fun getAnimeSeasonUpdate(
        anime: SAnime,
        seasons: List<SAnime>,
        fetchDetails: Boolean,
        fetchSeasons: Boolean,
    ): SAnimeSeasonUpdate = supervisorScope {
        val asyncAnime = if (fetchDetails) async { getOldAnimeDetails(anime) } else null
        val asyncSeasons = if (fetchSeasons) async { getOldSeasonList(anime) } else null
        SAnimeSeasonUpdate(asyncAnime?.await() ?: anime, asyncSeasons?.await() ?: seasons)
    }

    override val supportsRelatedAnime = false

    override suspend fun getRelatedAnimeList(anime: SAnime): List<AnimeRelation> = emptyList()

    override suspend fun getAnimeEpisodeUpdate(
        anime: SAnime,
        episodes: List<SEpisode>,
        fetchDetails: Boolean,
        fetchEpisodes: Boolean,
    ): SAnimeEpisodeUpdate = supervisorScope {
        val asyncAnime = if (fetchDetails) async { getOldAnimeDetails(anime) } else null
        val asyncEpisodes = if (fetchEpisodes) async { getOldEpisodeList(anime) } else null
        SAnimeEpisodeUpdate(asyncAnime?.await() ?: anime, asyncEpisodes?.await() ?: episodes)
    }

    // Anime details related
    private suspend fun getOldAnimeDetails(anime: SAnime): SAnime = withIOContext {
        val animeDirFiles = fileSystem.getFilesInAnimeDirectory(anime.url)

        animeDirFiles.firstOrNull {
            it.isFile && it.nameWithoutExtension.equals("cover", ignoreCase = true)
        }?.let {
            anime.thumbnail_url = it.uri.toString()
        }

        animeDirFiles.firstOrNull {
            it.isFile && it.nameWithoutExtension.equals("background", ignoreCase = true)
        }?.let {
            anime.background_url = it.uri.toString()
        }

        animeDirFiles
            .firstOrNull { it.extension == "json" && it.nameWithoutExtension == "details" }
            ?.let { file ->
                json.decodeFromStream<AnimeDetails>(file.openInputStream()).run {
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

    // Seasons
    private suspend fun getOldSeasonList(anime: SAnime): List<SAnime> = withIOContext {
        val animeDirs = fileSystem.getFilesInAnimeDirectory(anime.url)
            // Filter out files that are hidden and is not a folder
            .filter { it.isDirectory && !it.name.orEmpty().startsWith('.') }
            .distinctBy { it.name }

        animeDirs
            .map { animeDir ->
                async {
                    val url = animeDir.name?.let { season ->
                        buildString {
                            append(anime.url)
                            append(File.separator)
                            append(season)
                        }
                    }
                    getSAnime(url)
                }
            }
            .awaitAll()
            .toList()
    }

    // Episodes
    private suspend fun getOldEpisodeList(anime: SAnime): List<SEpisode> = withIOContext {
        val animeDir = fileSystem.getAnimeDirectory(anime.url)
        val filesInAnimeDir = animeDir?.listFiles().orEmpty().toList()
        val thumbnailsDirFiles = animeDir?.findFile(THUMBNAILS_DIR)?.listFiles().orEmpty().toList()

        val episodesData = filesInAnimeDir
            .firstOrNull {
                it.extension == "json" && it.nameWithoutExtension == "episodes"
            }?.let { file ->
                json.decodeFromStream<List<EpisodeDetails>>(file.openInputStream())
            }

        val thumbnailFilesMap = (thumbnailsDirFiles + filesInAnimeDir)
            .filter { it.isFile }
            .associateBy { it.nameWithoutExtension.orEmpty().lowercase() }

        val episodes = filesInAnimeDir
            // Only keep supported formats
            .filterNot { it.name.orEmpty().startsWith('.') }
            .filter { ArchiveAnime.isSupported(it) }
            .map { episodeFile ->
                SEpisode.create().apply {
                    url = "${anime.url}/${episodeFile.name}"
                    name = episodeFile.nameWithoutExtension.orEmpty()
                    date_upload = episodeFile.lastModified()

                    val episodeNumber = EpisodeRecognition.parseEpisodeNumber(
                        anime.title,
                        this.name,
                        this.episode_number.toDouble(),
                    ).toFloat()
                    episode_number = episodeNumber

                    // Overwrite data from episodes.json file
                    episodesData?.also { dataList ->
                        dataList.firstOrNull { it.episode_number.equalsTo(episodeNumber) }?.also { data ->
                            data.name?.also { name = it }
                            data.date_upload?.also { date_upload = parseDate(it) }
                            scanlator = data.scanlator
                            summary = data.summary
                        }
                    }

                    // Generate the preview from the episode if available
                    val thumbnailName = "${this.name}-$DEFAULT_THUMBNAIL_NAME".substringBeforeLast('.').lowercase()
                    val thumbnailFile = thumbnailFilesMap[thumbnailName]

                    if (thumbnailFile != null) {
                        this.preview_url = thumbnailFile.uri.toString()
                    }
                }
            }
            .sortedWith { e1, e2 ->
                val e = e2.episode_number.compareTo(e1.episode_number)
                if (e == 0) e2.name.compareToCaseInsensitiveNaturalOrder(e1.name) else e
            }

        val missingThumbnails = episodes.filter { it.preview_url.isNullOrBlank() }
        if (missingThumbnails.isNotEmpty() && libraryPreferences.generateLocalThumbnails().get()) {
            val dbAnime = getAnimeByUrlAndSourceId.await(anime.url, ID)
            val sortDescending = dbAnime?.sortDescending() ?: true
            val orderedMissingThumbnails = if (sortDescending) missingThumbnails else missingThumbnails.reversed()

            orderedMissingThumbnails.forEach { ep ->
                thumbnailScope.launch {
                    thumbnailSemaphore.withPermit {
                        try {
                            val existingThumbnail = thumbnailManager.find(
                                anime.url,
                                "${ep.name}-$DEFAULT_THUMBNAIL_NAME",
                            )
                            val resultFile = if (existingThumbnail != null) {
                                existingThumbnail
                            } else {
                                val tempFileSuffix = anime.title + ep.name + DEFAULT_THUMBNAIL_NAME
                                var updatedFile: UniFile? = null
                                val updateThumbnail: (InputStream) -> Unit = { inputStream ->
                                    updatedFile = thumbnailManager.update(anime, ep, inputStream)
                                }
                                updateImageFromVideo(ep, anime, tempFileSuffix, updateThumbnail)
                                updatedFile
                            }

                            if (resultFile != null) {
                                val previewUri = resultFile.uri.toString()
                                ep.preview_url = previewUri
                                val dbAnime = getAnimeByUrlAndSourceId.await(anime.url, ID)

                                if (dbAnime != null) {
                                    val dbEpisode = getEpisodeByUrlAndAnimeId.await(ep.url, dbAnime.id)
                                    if (dbEpisode != null) {
                                        updateEpisode.await(
                                            EpisodeUpdate(
                                                id = dbEpisode.id,
                                                previewUrl = previewUri,
                                            ),
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR) { "Couldn't extract thumbnail from video: $e" }
                        }
                    }
                }
            }
        }

        // Generate the cover from the first episode found if not available
        val coverFile = filesInAnimeDir.firstOrNull {
            it.isFile &&
                it.nameWithoutExtension.equals("cover", ignoreCase = true)
        }
        if (coverFile != null) {
            anime.thumbnail_url = coverFile.uri.toString()
        } else if (libraryPreferences.generateLocalThumbnails().get()) {
            episodes.lastOrNull()?.let { episode ->
                thumbnailScope.launch {
                    thumbnailSemaphore.withPermit {
                        try {
                            val currentCover = coverManager.find(anime.url)
                            val resultCoverFile = if (currentCover != null) {
                                currentCover
                            } else {
                                val tempFileSuffix = anime.title + DEFAULT_COVER_NAME
                                val updateCover: (InputStream) -> Unit = { coverManager.update(anime, it) }
                                updateImageFromVideo(episode, anime, tempFileSuffix, updateCover, 600)
                                coverManager.find(anime.url)
                            }

                            if (resultCoverFile != null) {
                                val coverUri = resultCoverFile.uri.toString()
                                anime.thumbnail_url = coverUri
                                val dbAnime = getAnimeByUrlAndSourceId.await(anime.url, ID)
                                if (dbAnime != null) {
                                    animeRepository.updateAnime(
                                        AnimeUpdate(
                                            id = dbAnime.id,
                                            thumbnailUrl = coverUri,
                                            coverLastModified = Instant.now().toEpochMilli(),
                                        ),
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR) { "Couldn't extract cover from video: $e" }
                        }
                    }
                }
            }
        }

        // Generate the background from the first episode found if not available
        val backgroundFile = filesInAnimeDir.firstOrNull {
            it.isFile &&
                it.nameWithoutExtension.equals("background", ignoreCase = true)
        }

        if (backgroundFile != null) {
            anime.background_url = backgroundFile.uri.toString()
        } else if (libraryPreferences.generateLocalThumbnails().get()) {
            episodes.lastOrNull()?.let { episode ->
                thumbnailScope.launch {
                    thumbnailSemaphore.withPermit {
                        try {
                            val currentBg = backgroundManager.find(anime.url)
                            val resultBgFile = if (currentBg != null) {
                                currentBg
                            } else {
                                val tempFileSuffix = anime.title + DEFAULT_BACKGROUND_NAME
                                val updateBackground: (InputStream) -> Unit = { backgroundManager.update(anime, it) }
                                updateImageFromVideo(episode, anime, tempFileSuffix, updateBackground, 960)
                                backgroundManager.find(anime.url)
                            }

                            if (resultBgFile != null) {
                                val bgUri = resultBgFile.uri.toString()
                                anime.background_url = bgUri
                                val dbAnime = getAnimeByUrlAndSourceId.await(anime.url, ID)
                                if (dbAnime != null) {
                                    animeRepository.updateAnime(
                                        AnimeUpdate(
                                            id = dbAnime.id,
                                            backgroundUrl = bgUri,
                                            backgroundLastModified = Instant.now().toEpochMilli(),
                                        ),
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR) { "Couldn't extract background from video: $e" }
                        }
                    }
                }
            }
        }

        episodes
    }

    private fun parseDate(isoDate: String): Long {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(isoDate)?.time ?: 0L
    }

    private fun Float.equalsTo(other: Float): Boolean {
        return abs(this - other) < 0.0001
    }

    // Filters
    override fun getFilterList() = AnimeFilterList(AnimeOrderBy.Popular(context))

    // Unused stuff
    override suspend fun getVideoList(episode: SEpisode): List<Video> = throw UnsupportedOperationException("Unused")

    private fun updateImageFromVideo(
        episode: SEpisode,
        anime: SAnime,
        tempFileSuffix: String,
        updateImage: (InputStream) -> Unit,
        targetWidth: Int = 720,
    ) {
        val tempFile = File.createTempFile(
            "tmp_",
            tempFileSuffix,
        )
        try {
            val outFile = tempFile.path

            val episodeName = episode.url.split('/', limit = 2).last()
            val animeDir = fileSystem.getAnimeDirectory(anime.url)!!
            val episodeFile = animeDir.findFile(episodeName)!!
            val episodeFilename = { episodeFile.toFFmpegString(context) }

            val ffProbe = com.arthenica.ffmpegkit.FFprobeKit.execute(
                "-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 \"${episodeFilename()}\"",
            )
            val duration = ffProbe.allLogsAsString.trim().toFloatOrNull() ?: 120f
            val second = (duration.toInt() / 2).coerceAtLeast(1)

            com.arthenica.ffmpegkit.FFmpegKit.execute(
                "-ss $second -noaccurate_seek -i \"${episodeFilename()}\" -vf \"scale='min($targetWidth,iw)':-2\" -q:v 3 -frames:v 1 -update true \"$outFile\" -y",
            )

            if (tempFile.length() > 0L) {
                updateImage(tempFile.inputStream())
            }
        } finally {
            tempFile.delete()
        }
    }

    companion object {
        const val ID = 0L
        const val HELP_URL = "https://aniyomi.org/help/guides/local-anime/"

        internal const val DEFAULT_COVER_NAME = "cover.jpg"
        internal const val DEFAULT_BACKGROUND_NAME = "background.jpg"
        internal const val DEFAULT_THUMBNAIL_NAME = "thumbnail.jpg"
        internal const val THUMBNAILS_DIR = ".thumbnails"

        private val LATEST_THRESHOLD = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS)
    }
}

fun Anime.isLocal(): Boolean = source == LocalAnimeSource.ID

fun AnimeSource.isLocal(): Boolean = id == LocalAnimeSource.ID
