package tachiyomi.source.local.io.manga

import java.io.File

expect class LocalMangaSourceFileSystem {

    fun getBaseDirectories(): Sequence<File>

    fun getFilesInBaseDirectories(): Sequence<File>

    fun getMangaDirectory(name: String): File?

    fun getFilesInMangaDirectory(name: String): Sequence<File>
}
