package tachiyomi.source.local.image.anime

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.local.io.anime.LocalAnimeSourceFileSystem
import java.io.InputStream

private const val DEFAULT_THUMBNAIL_NAME = "thumbnail.jpg"

actual class LocalEpisodeThumbnailManager(
    private val context: Context,
    private val fileSystem: LocalAnimeSourceFileSystem,
) {

    actual fun find(animeUrl: String, fileName: String): UniFile? {
        return fileSystem.getFilesInAnimeDirectory(animeUrl)
            // Get all file whose names contain the episode name and the word 'thumbnail'
            .filter { it.isFile && it.nameWithoutExtension.equals(fileName, ignoreCase = true) }
            // Get the first actual image
            .firstOrNull { ImageUtil.isImage(it.name) { it.openInputStream() } }
    }

    actual fun update(anime: SAnime, episode: SEpisode, inputStream: InputStream): UniFile? {
        val directory = fileSystem.getAnimeDirectory(anime.url)
        if (directory == null) {
            inputStream.close()
            return null
        }

        val fileName = "${episode.name}-$DEFAULT_THUMBNAIL_NAME"
        val targetFile = find(anime.url, fileName) ?: directory.createFile(fileName)!!

        inputStream.use { input ->
            targetFile.openOutputStream().use { output ->
                input.copyTo(output)
            }
        }

        DiskUtil.createNoMediaFile(directory, context)

        episode.preview_url = targetFile.uri.toString()
        return targetFile
    }
}
