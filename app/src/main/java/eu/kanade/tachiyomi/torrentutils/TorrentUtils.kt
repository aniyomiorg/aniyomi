package eu.kanade.tachiyomi.torrentutils

import eu.kanade.tachiyomi.data.torrentServer.TorrentServerApi
import eu.kanade.tachiyomi.torrentutils.model.DeadTorrentException
import eu.kanade.tachiyomi.torrentutils.model.TorrentFile
import eu.kanade.tachiyomi.torrentutils.model.TorrentInfo
import java.net.SocketTimeoutException

/**
 * Used by extensions.
 */
@Suppress("UNUSED")
object TorrentUtils {
    suspend fun getTorrentInfo(
        url: String,
        title: String,
    ): TorrentInfo {
        @Suppress("SwallowedException")
        try {
            val torrent = TorrentServerApi.addTorrent(url, title, "", "", false)
            return TorrentInfo(
                torrent.title,
                torrent.file_stats?.map { file ->
                    TorrentFile(file.path, file.id ?: 0, file.length, torrent.hash!!, torrent.trackers ?: emptyList())
                } ?: emptyList(),
                torrent.hash!!,
                torrent.torrent_size!!,
                torrent.trackers ?: emptyList(),
            )
        } catch (e: SocketTimeoutException) {
            throw DeadTorrentException()
        }
    }
}
