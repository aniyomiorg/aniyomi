package tachiyomi.source.local.io.anime

import com.hippo.unifile.UniFile
import tachiyomi.domain.storage.service.StorageManager

actual class LocalAnimeSourceFileSystem(
    private val storageManager: StorageManager,
) {

    actual fun getBaseDirectory(): UniFile? {
        return storageManager.getLocalAnimeSourceDirectory()
    }

    actual fun getFilesInBaseDirectory(): List<UniFile> {
        return getBaseDirectory()?.listFiles().orEmpty().toList()
    }

    actual fun getAnimeDirectory(name: String): UniFile? {
        return getBaseDirectory()
            ?.findFile(name)
            ?.takeIf { it.isDirectory }
    }

    actual fun getFilesInAnimeDirectory(name: String): List<UniFile> {
        return getBaseDirectory()
            ?.findFile(name)
            ?.takeIf { it.isDirectory }
            ?.listFiles().orEmpty().toList()
    }
}
