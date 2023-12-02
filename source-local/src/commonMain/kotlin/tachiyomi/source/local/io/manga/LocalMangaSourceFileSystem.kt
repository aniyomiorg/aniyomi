package tachiyomi.source.local.io.manga

import com.hippo.unifile.UniFile

expect class LocalMangaSourceFileSystem {

    fun getBaseDirectory(): UniFile?

    fun getFilesInBaseDirectory(): List<UniFile>

    fun getMangaDirectory(name: String): UniFile?

    fun getFilesInMangaDirectory(name: String): List<UniFile>
}
