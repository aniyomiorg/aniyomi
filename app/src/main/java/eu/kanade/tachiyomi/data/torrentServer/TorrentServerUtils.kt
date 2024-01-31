package eu.kanade.tachiyomi.data.torrentServer

import eu.kanade.tachiyomi.data.torrentServer.model.FileStat
import eu.kanade.tachiyomi.data.torrentServer.model.Torrent
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.net.URLEncoder

object TorrentServerUtils {
    private val preferences: TorrentServerPreferences by injectLazy()
    val hostUrl = "http://127.0.0.1:${preferences.port().get()}"

    private val animeTrackers = """http://nyaa.tracker.wf:7777/announce,
            http://anidex.moe:6969/announce,http://tracker.anirena.com:80/announce,
            udp://tracker.uw0.xyz:6969/announce,
            http://share.camoe.cn:8080/announce,
            http://t.nyaatracker.com:80/announce,
            udp://47.ip-51-68-199.eu:6969/announce,
            udp://9.rarbg.me:2940,
            udp://9.rarbg.to:2820,
            udp://exodus.desync.com:6969/announce,
            udp://explodie.org:6969/announce,
            udp://ipv4.tracker.harry.lu:80/announce,
            udp://open.stealth.si:80/announce,
            udp://opentor.org:2710/announce,
            udp://opentracker.i2p.rocks:6969/announce,
            udp://retracker.lanta-net.ru:2710/announce,
            udp://tracker.cyberia.is:6969/announce,
            udp://tracker.dler.org:6969/announce,
            udp://tracker.ds.is:6969/announce,
            udp://tracker.internetwarriors.net:1337,
            udp://tracker.openbittorrent.com:6969/announce,
            udp://tracker.opentrackr.org:1337/announce,
            udp://tracker.tiny-vps.com:6969/announce,
            udp://tracker.torrent.eu.org:451/announce,
            udp://valakas.rollo.dnsabr.com:2710/announce,
            udp://www.torrent.eu.org:451/announce
        """.trimIndent()

    fun getTrackerList(trackerList: String = ""): String {
        val mergedTrackerList = "$animeTrackers, $trackerList".trimIndent()
        return mergedTrackerList.split(",").map { it.trim() }.filter { it.isNotBlank() }.joinToString("&tr=")
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
