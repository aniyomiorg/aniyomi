package tachiyomi.source.local.io.anime

import android.content.Context
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.source.local.R
import java.io.File

actual class LocalAnimeSourceFileSystem(
    private val context: Context,
) {

    private val baseFolderLocation = "${context.getString(R.string.app_name)}${File.separator}localanime"

    actual fun getBaseDirectories(): Sequence<File> {
        return DiskUtil.getExternalStorages(context)
            .map { File(it.absolutePath, baseFolderLocation) }
            .asSequence()
    }

    actual fun getFilesInBaseDirectories(): Sequence<File> {
        return getBaseDirectories()
            // Get all the files inside all baseDir
            .flatMap { it.listFiles().orEmpty().toList() }
    }

    actual fun getAnimeDirectory(name: String): File? {
        return getFilesInBaseDirectories()
            // Get the first animeDir or null
            .firstOrNull { it.isDirectory && it.name == name }
    }

    actual fun getFilesInAnimeDirectory(name: String): Sequence<File> {
        return getFilesInBaseDirectories()
            // Filter out ones that are not related to the anime and is not a directory
            .filter { it.isDirectory && it.name == name }
            // Get all the files inside the filtered folders
            .flatMap { it.listFiles().orEmpty().toList() }
    }
}
