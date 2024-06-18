package eu.kanade.tachiyomi.torrentutils

import eu.kanade.tachiyomi.data.torrentServer.TorrentServerApi
import eu.kanade.tachiyomi.torrentutils.model.TorrentFile
import eu.kanade.tachiyomi.torrentutils.model.TorrentInfo

/**
 * Used by extensions.
 */
@Suppress("UNUSED")
object TorrentUtils {
    suspend fun getTorrentInfo(
        url: String,
        title: String,
    ): TorrentInfo {
        val torrent = TorrentServerApi.addTorrent(url, title, "", "", false)
        return TorrentInfo(
            torrent.title,
            torrent.file_stats?.map { file ->
                TorrentFile(file.path, file.id ?: 0, file.length, torrent.hash!!)
            } ?: emptyList(),
            torrent.hash!!,
            torrent.torrent_size!!,
            torrent.trackers ?: emptyList(),
        )
    }

    fun getTorrentPlayUrl(
        torrent: TorrentInfo,
        indexFile: Int = 0,
    ): String {
        return torrent.files[indexFile].toVideoUrl()
    }
}
