package tachiyomi.source.local.io.anime

import java.io.File

expect class LocalAnimeSourceFileSystem {

    fun getBaseDirectory(): File

    fun getFilesInBaseDirectory(): List<File>

    fun getAnimeDirectory(name: String): File?

    fun getFilesInAnimeDirectory(name: String): List<File>
}
