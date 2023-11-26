package tachiyomi.source.local.io.manga

import java.io.File

expect class LocalMangaSourceFileSystem {

    fun getBaseDirectory(): File

    fun getFilesInBaseDirectory(): List<File>

    fun getMangaDirectory(name: String): File?

    fun getFilesInMangaDirectory(name: String): List<File>
}
