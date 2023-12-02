package tachiyomi.source.local.io.anime

import com.hippo.unifile.UniFile

expect class LocalAnimeSourceFileSystem {

    fun getBaseDirectory(): UniFile?

    fun getFilesInBaseDirectory(): List<UniFile>

    fun getAnimeDirectory(name: String): UniFile?

    fun getFilesInAnimeDirectory(name: String): List<UniFile>
}
