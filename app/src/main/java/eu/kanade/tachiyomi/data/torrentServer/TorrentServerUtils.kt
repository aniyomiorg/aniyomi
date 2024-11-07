package eu.kanade.tachiyomi.data.torrentServer

import eu.kanade.tachiyomi.data.torrentServer.model.FileStat
import eu.kanade.tachiyomi.data.torrentServer.model.Torrent
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.net.URLEncoder

object TorrentServerUtils {
    private val preferences: TorrentServerPreferences by injectLazy()
    val hostUrl = "http://127.0.0.1:${preferences.port().get()}"

    // Is necessary separate the trackers by comma because is hardcoded in go-torrent-server
    private val animeTrackers = preferences.trackers().get().split("\n").joinToString(",\n")

    fun setTrackersList() {
        torrServer.TorrServer.addTrackers(animeTrackers)
    }

    fun getTorrentPlayLink(torr: Torrent, index: Int): String {
        val file = findFile(torr, index)
        val name = file?.let { File(it.path).name } ?: torr.title
        return "$hostUrl/stream/${name.urlEncode()}?link=${torr.hash}&index=$index&play"
    }

    private fun findFile(torrent: Torrent, index: Int): FileStat? {
        torrent.file_stats?.forEach {
            if (it.id == index) {
                return it
            }
        }
        return null
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "utf8")
}
