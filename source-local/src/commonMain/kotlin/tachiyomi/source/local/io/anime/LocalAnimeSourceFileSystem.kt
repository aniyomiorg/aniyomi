package tachiyomi.source.local.io.anime

import java.io.File

expect class LocalAnimeSourceFileSystem {

    fun getBaseDirectories(): Sequence<File>

    fun getFilesInBaseDirectories(): Sequence<File>

    fun getAnimeDirectory(name: String): File?

    fun getFilesInAnimeDirectory(name: String): Sequence<File>
}
