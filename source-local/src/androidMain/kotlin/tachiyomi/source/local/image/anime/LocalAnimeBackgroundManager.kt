package tachiyomi.source.local.image.anime

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.local.io.anime.LocalAnimeSourceFileSystem
import java.io.InputStream

private const val DEFAULT_BACKGROUND_NAME = "background.jpg"

actual class LocalAnimeBackgroundManager(
    private val context: Context,
    private val fileSystem: LocalAnimeSourceFileSystem,
) {

    actual fun find(animeUrl: String): UniFile? {
        return fileSystem.getFilesInAnimeDirectory(animeUrl)
            // Get all file whose names start with 'background'
            .filter { it.isFile && it.nameWithoutExtension.equals("background", ignoreCase = true) }
            // Get the first actual image
            .firstOrNull { ImageUtil.isImage(it.name) { it.openInputStream() } }
    }

    actual fun update(anime: SAnime, inputStream: InputStream): UniFile? {
        val directory = fileSystem.getAnimeDirectory(anime.url)
        if (directory == null) {
            inputStream.close()
            return null
        }

        val targetFile = find(anime.url) ?: directory.createFile(DEFAULT_BACKGROUND_NAME)!!

        inputStream.use { input ->
            targetFile.openOutputStream().use { output ->
                input.copyTo(output)
            }
        }

        DiskUtil.createNoMediaFile(directory, context)

        anime.background_url = targetFile.uri.toString()
        return targetFile
    }
}
