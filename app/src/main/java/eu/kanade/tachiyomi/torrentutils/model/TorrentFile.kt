package eu.kanade.tachiyomi.torrentutils.model

import eu.kanade.tachiyomi.data.torrentServer.TorrentServerUtils
import java.io.File
import java.net.URLEncoder

data class TorrentFile(
    val path: String,
    val indexFile: Int,
    val size: Long,
    private val torrentHash: String,
    private val trackers : List<String> = emptyList(),
) {
    fun toMagnetURI(): String {
        val trackers = trackers.joinToString("&tr=") { URLEncoder.encode(it, "UTF-8") }
        return "magnet:?xt=urn:btih:$torrentHash${if (trackers.isNotEmpty()) "&tr=$trackers" else ""}&index=$indexFile"
    }
}
