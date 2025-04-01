package eu.kanade.tachiyomi.torrentServer

import eu.kanade.tachiyomi.torrentServer.model.FileStat
import eu.kanade.tachiyomi.torrentServer.model.Torrent
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URLEncoder

object TorrentServerUtils {
    private val preferences: TorrentServerPreferences by injectLazy()

    val hostUrl = "http://${getLocalIpAddress()}:${preferences.port().get()}"

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

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (ex: Exception) {
            logcat(LogPriority.DEBUG) { "Error getting local IP address" }
        }
        return "127.0.0.1"
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "utf8")
}
