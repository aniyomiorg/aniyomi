package tachiyomi.source.local.image.anime

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.core.util.system.ImageUtil
import tachiyomi.source.local.io.anime.LocalAnimeSourceFileSystem
import java.io.File
import java.io.InputStream

private const val DEFAULT_COVER_NAME = "cover.jpg"

actual class LocalAnimeCoverManager(
    private val context: Context,
    private val fileSystem: LocalAnimeSourceFileSystem,
) {

    actual fun find(animeUrl: String): File? {
        return fileSystem.getFilesInAnimeDirectory(animeUrl)
            // Get all file whose names start with 'cover'
            .filter { it.isFile && it.nameWithoutExtension.equals("cover", ignoreCase = true) }
            // Get the first actual image
            .firstOrNull {
                ImageUtil.isImage(it.name) { it.inputStream() }
            }
    }

    actual fun update(anime: SAnime, inputStream: InputStream): File? {
        val directory = fileSystem.getAnimeDirectory(anime.url)
        if (directory == null) {
            inputStream.close()
            return null
        }

        var targetFile = find(anime.url)
        if (targetFile == null) {
            targetFile = File(directory.absolutePath, DEFAULT_COVER_NAME)
            targetFile.createNewFile()
        }

        // It might not exist at this point
        targetFile.parentFile?.mkdirs()
        inputStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        DiskUtil.createNoMediaFile(UniFile.fromFile(directory), context)

        anime.thumbnail_url = targetFile.absolutePath
        return targetFile
    }
}
