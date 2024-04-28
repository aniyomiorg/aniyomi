package eu.kanade.tachiyomi.torrentutils

import eu.kanade.tachiyomi.data.torrentServer.TorrentServerApi
import eu.kanade.tachiyomi.data.torrentServer.TorrentServerUtils
import eu.kanade.tachiyomi.torrentutils.model.Torrent


/**
 * Used by extensions.
 */
@Suppress("UNUSED")
object TorrentUtils {

    fun getTorrent(url: String, title: String): Torrent {
        return TorrentServerApi.addTorrent(url, title, "", "", false)
    }

     fun getTorrentPlayUrl(torrent: Torrent, indexFile: Int): String {
        return TorrentServerUtils.getTorrentPlayLink(torrent, indexFile)
    }

}
