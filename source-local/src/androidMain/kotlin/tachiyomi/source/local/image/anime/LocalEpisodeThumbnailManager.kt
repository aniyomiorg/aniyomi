package tachiyomi.source.local.image.anime

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.local.entries.anime.LocalAnimeSource
import tachiyomi.source.local.io.anime.LocalAnimeSourceFileSystem
import java.io.InputStream

actual class LocalEpisodeThumbnailManager(
    private val context: Context,
    private val fileSystem: LocalAnimeSourceFileSystem,
) {

    actual fun find(animeUrl: String, fileName: String): UniFile? {
        val animeDir = fileSystem.getAnimeDirectory(animeUrl)
        val thumbnailsDir = animeDir?.findFile(LocalAnimeSource.THUMBNAILS_DIR)
        val files = thumbnailsDir?.listFiles().orEmpty().toList() +
            fileSystem.getFilesInAnimeDirectory(animeUrl)

        val nameToMatch = fileName.substringBeforeLast('.')

        return files
            // Get all file whose names match the episode thumbnail name
            .filter {
                it.isFile &&
                    (
                        it.name.equals(fileName, ignoreCase = true) ||
                            it.nameWithoutExtension.equals(nameToMatch, ignoreCase = true)
                        )
            }
            // Get the first actual image
            .firstOrNull { ImageUtil.isImage(it.name) { it.openInputStream() } }
    }

    actual fun update(anime: SAnime, episode: SEpisode, inputStream: InputStream): UniFile? {
        val directory = fileSystem.getAnimeDirectory(anime.url)
        if (directory == null) {
            inputStream.close()
            return null
        }

        val thumbnailsDir = directory.createDirectory(LocalAnimeSource.THUMBNAILS_DIR)!!
        val fileName = "${episode.name}-${LocalAnimeSource.DEFAULT_THUMBNAIL_NAME}"
        val targetFile = find(anime.url, fileName) ?: thumbnailsDir.createFile(fileName)!!

        inputStream.use { input ->
            targetFile.openOutputStream().use { output ->
                input.copyTo(output)
            }
        }

        DiskUtil.createNoMediaFile(thumbnailsDir, context)

        episode.preview_url = targetFile.uri.toString()
        return targetFile
    }
}
