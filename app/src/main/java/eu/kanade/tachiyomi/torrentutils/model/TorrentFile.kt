package eu.kanade.tachiyomi.torrentutils.model

import eu.kanade.tachiyomi.data.torrentServer.TorrentServerUtils
import java.io.File
import java.net.URLEncoder

data class TorrentFile(
    val path: String,
    val indexFile: Int,
    val size: Long,
    private val torrentHash: String,
) {
    override fun toString(): String {
        return path
    }

    private fun getFileName(): String {
        return File(path).name
    }

    fun toVideoUrl(): String {
        val encodedName = URLEncoder.encode(getFileName(), "utf8")
        return "${TorrentServerUtils.hostUrl}/stream/$encodedName?link=$torrentHash&index=$indexFile&play"
    }
}
