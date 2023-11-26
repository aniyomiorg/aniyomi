package tachiyomi.source.local.io.anime

import tachiyomi.core.provider.FolderProvider
import java.io.File

actual class LocalAnimeSourceFileSystem(
    private val folderProvider: FolderProvider,
) {

    actual fun getBaseDirectory(): File {
        return File(folderProvider.directory(), "localanime")
    }

    actual fun getFilesInBaseDirectory(): List<File> {
        return getBaseDirectory().listFiles().orEmpty().toList()
    }

    actual fun getAnimeDirectory(name: String): File? {
        return getFilesInBaseDirectory()
            // Get the first animeDir or null
            .firstOrNull { it.isDirectory && it.name == name }
    }

    actual fun getFilesInAnimeDirectory(name: String): List<File> {
        return getFilesInBaseDirectory()
            // Filter out ones that are not related to the anime and is not a directory
            .filter { it.isDirectory && it.name == name }
            // Get all the files inside the filtered folders
            .flatMap { it.listFiles().orEmpty().toList() }
    }
}
