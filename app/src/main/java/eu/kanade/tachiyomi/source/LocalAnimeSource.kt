package eu.kanade.tachiyomi.source

import android.content.Context
import com.github.junrar.Archive
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.AnimesPage
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SEpisode
import eu.kanade.tachiyomi.util.episode.EpisodeRecognition
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import eu.kanade.tachiyomi.util.storage.AnimeFile
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.ImageUtil
import rx.Observable
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

class LocalAnimeSource(private val context: Context) : AnimeCatalogueSource {
    companion object {
        const val ID = 0L
        const val HELP_URL = "https://tachiyomi.org/help/guides/local-anime/"

        private const val COVER_NAME = "cover.jpg"
        private val SUPPORTED_ARCHIVE_TYPES = setOf("zip", "rar", "cbr", "cbz", "epub")

        private val LATEST_THRESHOLD = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS)

        fun updateCover(context: Context, anime: SAnime, input: InputStream): File? {
            val dir = getBaseDirectories(context).firstOrNull()
            if (dir == null) {
                input.close()
                return null
            }
            val cover = File("${dir.absolutePath}/${anime.url}", COVER_NAME)

            // It might not exist if using the external SD card
            cover.parentFile?.mkdirs()
            input.use {
                cover.outputStream().use {
                    input.copyTo(it)
                }
            }
            return cover
        }

        private fun getBaseDirectories(context: Context): List<File> {
            val c = context.getString(R.string.app_name) + File.separator + "local"
            return DiskUtil.getExternalStorages(context).map { File(it.absolutePath, c) }
        }
    }

    override val id = ID
    override val name = context.getString(R.string.local_source)
    override val lang = ""
    override val supportsLatest = true

    override fun toString() = context.getString(R.string.local_source)

    override fun fetchPopularAnime(page: Int) = fetchSearchAnime(page, "", POPULAR_FILTERS)

    override fun fetchSearchAnime(page: Int, query: String, filters: FilterList): Observable<AnimesPage> {
        val baseDirs = getBaseDirectories(context)

        val time = if (filters === LATEST_FILTERS) System.currentTimeMillis() - LATEST_THRESHOLD else 0L
        var animeDirs = baseDirs
            .asSequence()
            .mapNotNull { it.listFiles()?.toList() }
            .flatten()
            .filter { it.isDirectory }
            .filterNot { it.name.startsWith('.') }
            .filter { if (time == 0L) it.name.contains(query, ignoreCase = true) else it.lastModified() >= time }
            .distinctBy { it.name }

        val state = ((if (filters.isEmpty()) POPULAR_FILTERS else filters)[0] as OrderBy).state
        when (state?.index) {
            0 -> {
                animeDirs = if (state.ascending) {
                    animeDirs.sortedBy { it.name.toLowerCase(Locale.ENGLISH) }
                } else {
                    animeDirs.sortedByDescending { it.name.toLowerCase(Locale.ENGLISH) }
                }
            }
            1 -> {
                animeDirs = if (state.ascending) {
                    animeDirs.sortedBy(File::lastModified)
                } else {
                    animeDirs.sortedByDescending(File::lastModified)
                }
            }
        }

        val animes = animeDirs.map { animeDir ->
            SAnime.create().apply {
                title = animeDir.name
                url = animeDir.name

                // Try to find the cover
                for (dir in baseDirs) {
                    val cover = File("${dir.absolutePath}/$url", COVER_NAME)
                    if (cover.exists()) {
                        thumbnail_url = cover.absolutePath
                        break
                    }
                }

                val episodes = fetchEpisodeList(this).toBlocking().first()
                if (episodes.isNotEmpty()) {
                    val episode = episodes.last()
                    val format = getFormat(episode)
                    if (format is Format.Anime) {
                        AnimeFile(format.file).use { epub ->
                            epub.fillAnimeMetadata(this)
                        }
                    }

                    // Copy the cover from the first episode found.
                    if (thumbnail_url == null) {
                        try {
                            val dest = updateCover(episode, this)
                            thumbnail_url = dest?.absolutePath
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
            }
        }

        return Observable.just(AnimesPage(animes.toList(), false))
    }

    override fun fetchLatestUpdates(page: Int) = fetchSearchAnime(page, "", LATEST_FILTERS)

    override fun fetchAnimeDetails(anime: SAnime): Observable<SAnime> {
        getBaseDirectories(context)
            .asSequence()
            .mapNotNull { File(it, anime.url).listFiles()?.toList() }
            .flatten()
            .firstOrNull { it.extension == "json" }
            ?.apply {
                val reader = this.inputStream().bufferedReader()
                val json = JsonParser.parseReader(reader).asJsonObject

                anime.title = json["title"]?.asString ?: anime.title
                anime.author = json["author"]?.asString ?: anime.author
                anime.artist = json["artist"]?.asString ?: anime.artist
                anime.description = json["description"]?.asString ?: anime.description
                anime.genre = json["genre"]?.asJsonArray?.joinToString(", ") { it.asString }
                    ?: anime.genre
                anime.status = json["status"]?.asInt ?: anime.status
            }

        return Observable.just(anime)
    }

    override fun fetchEpisodeList(anime: SAnime): Observable<List<SEpisode>> {
        val episodes = getBaseDirectories(context)
            .asSequence()
            .mapNotNull { File(it, anime.url).listFiles()?.toList() }
            .flatten()
            .filter { it.isDirectory || isSupportedFile(it.extension) }
            .map { episodeFile ->
                SEpisode.create().apply {
                    url = "${anime.url}/${episodeFile.name}"
                    name = if (episodeFile.isDirectory) {
                        episodeFile.name
                    } else {
                        episodeFile.nameWithoutExtension
                    }
                    date_upload = episodeFile.lastModified()

                    val format = getFormat(this)
                    if (format is Format.Anime) {
                        AnimeFile(format.file).use { epub ->
                            epub.fillEpisodeMetadata(this)
                        }
                    }

                    val chapNameCut = stripAnimeTitle(name, anime.title)
                    if (chapNameCut.isNotEmpty()) name = chapNameCut
                    EpisodeRecognition.parseEpisodeNumber(this, anime)
                }
            }
            .sortedWith { e1, e2 ->
                val e = e2.episode_number.compareTo(e1.episode_number)
                if (e == 0) e2.name.compareToCaseInsensitiveNaturalOrder(e1.name) else e
            }
            .toList()

        return Observable.just(episodes)
    }

    override fun fetchEpisodeLink(episode: SEpisode): Observable<String> {
        val link = episode.url

        return Observable.just(link)
    }

    /**
     * Strips the anime title from a episode name, matching only based on alphanumeric and whitespace
     * characters.
     */
    private fun stripAnimeTitle(episodeName: String, animeTitle: String): String {
        var episodeNameIndex = 0
        var animeTitleIndex = 0
        while (episodeNameIndex < episodeName.length && animeTitleIndex < animeTitle.length) {
            val episodeChar = episodeName[episodeNameIndex]
            val animeChar = animeTitle[animeTitleIndex]
            if (!episodeChar.equals(animeChar, true)) {
                val invalidEpisodeChar = !episodeChar.isLetterOrDigit() && !episodeChar.isWhitespace()
                val invalidAnimeChar = !animeChar.isLetterOrDigit() && !animeChar.isWhitespace()

                if (!invalidEpisodeChar && !invalidAnimeChar) {
                    return episodeName
                }

                if (invalidEpisodeChar) {
                    episodeNameIndex++
                }

                if (invalidAnimeChar) {
                    animeTitleIndex++
                }
            } else {
                episodeNameIndex++
                animeTitleIndex++
            }
        }

        return episodeName.substring(episodeNameIndex).trimStart(' ', '-', '_', ',', ':')
    }

    private fun isSupportedFile(extension: String): Boolean {
        return extension.toLowerCase(Locale.ROOT) in SUPPORTED_ARCHIVE_TYPES
    }

    fun getFormat(episode: SEpisode): Format {
        val baseDirs = getBaseDirectories(context)

        for (dir in baseDirs) {
            val chapFile = File(dir, episode.url)
            if (!chapFile.exists()) continue

            return getFormat(chapFile)
        }
        throw Exception(context.getString(R.string.episode_not_found))
    }

    private fun getFormat(file: File): Format {
        val extension = file.extension
        return if (file.isDirectory) {
            Format.Directory(file)
        } else if (extension.equals("zip", true) || extension.equals("cbz", true)) {
            Format.Zip(file)
        } else if (extension.equals("rar", true) || extension.equals("cbr", true)) {
            Format.Rar(file)
        } else if (extension.equals("epub", true)) {
            Format.Anime(file)
        } else {
            throw Exception(context.getString(R.string.local_invalid_episode_format))
        }
    }

    private fun updateCover(episode: SEpisode, anime: SAnime): File? {
        return when (val format = getFormat(episode)) {
            is Format.Directory -> {
                val entry = format.file.listFiles()
                    ?.sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
                    ?.find { !it.isDirectory && ImageUtil.isImage(it.name) { FileInputStream(it) } }

                entry?.let { updateCover(context, anime, it.inputStream()) }
            }
            is Format.Zip -> {
                ZipFile(format.file).use { zip ->
                    val entry = zip.entries().toList()
                        .sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
                        .find { !it.isDirectory && ImageUtil.isImage(it.name) { zip.getInputStream(it) } }

                    entry?.let { updateCover(context, anime, zip.getInputStream(it)) }
                }
            }
            is Format.Rar -> {
                Archive(format.file).use { archive ->
                    val entry = archive.fileHeaders
                        .sortedWith { f1, f2 -> f1.fileName.compareToCaseInsensitiveNaturalOrder(f2.fileName) }
                        .find { !it.isDirectory && ImageUtil.isImage(it.fileName) { archive.getInputStream(it) } }

                    entry?.let { updateCover(context, anime, archive.getInputStream(it)) }
                }
            }
            is Format.Anime -> {
                AnimeFile(format.file).use { epub ->
                    val entry = epub.getImagesFromPages()
                        .firstOrNull()
                        ?.let { epub.getEntry(it) }

                    entry?.let { updateCover(context, anime, epub.getInputStream(it)) }
                }
            }
        }
    }

    override fun getFilterList() = POPULAR_FILTERS

    private val POPULAR_FILTERS = FilterList(OrderBy(context))
    private val LATEST_FILTERS = FilterList(OrderBy(context).apply { state = Filter.Sort.Selection(1, false) })

    private class OrderBy(context: Context) : Filter.Sort(
        context.getString(R.string.local_filter_order_by),
        arrayOf(context.getString(R.string.title), context.getString(R.string.date)),
        Selection(0, true)
    )

    sealed class Format {
        data class Directory(val file: File) : Format()
        data class Zip(val file: File) : Format()
        data class Rar(val file: File) : Format()
        data class Anime(val file: File) : Format()
    }
}
