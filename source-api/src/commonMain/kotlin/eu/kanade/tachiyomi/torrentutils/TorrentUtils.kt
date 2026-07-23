package eu.kanade.tachiyomi.torrentutils

import aniyomi.core.common.torrent.DisabledTorrServerException
import aniyomi.core.common.torrent.TorrentHelpers
import aniyomi.core.common.torrent.TorrentServerApi
import aniyomi.core.common.torrent.model.Torrent
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.get
import eu.kanade.tachiyomi.torrentutils.model.DeadTorrentException
import eu.kanade.tachiyomi.torrentutils.model.TorrentFile
import eu.kanade.tachiyomi.torrentutils.model.TorrentInfo
import uy.kohesive.injekt.injectLazy
import java.net.SocketTimeoutException

object TorrentUtils {
    private val torrentServerApi: TorrentServerApi by injectLazy()
    private val network: NetworkHelper by injectLazy()

    suspend fun getTorrentInfo(
        url: String,
        title: String,
    ): TorrentInfo {
            val torrent: Torrent = if (url.startsWith("magnet")) {
                // Magnet links need to be added to the torrent server to retrieve their
                // information
                try {
                    torrentServerApi.addTorrent(url, title, "", "", false)
                } catch (_: SocketTimeoutException) {
                    throw DeadTorrentException()
                } catch (_: Exception) {
                    throw DisabledTorrServerException()
                }
            } else {
                // For torrent files we can parse the information out of the file itself
                // without starting the torrent server
                TorrentHelpers.parseTorrentFromFile(network.client.get(url).body.byteStream())
            }
            return torrentToTorrentInfo(torrent, title)
    }

    private fun torrentToTorrentInfo(torrent: Torrent, overrideTitle: String?): TorrentInfo {
        return TorrentInfo(
            overrideTitle ?: torrent.title,
            torrent.fileStats?.map { file ->
                TorrentFile(file.path, file.id ?: 0, file.length, torrent.hash!!, torrent.trackers ?: emptyList())
            } ?: emptyList(),
            torrent.hash!!,
            torrent.torrentSize!!,
            torrent.trackers ?: emptyList(),
        )
    }
}
