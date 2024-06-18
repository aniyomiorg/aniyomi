package eu.kanade.tachiyomi.torrentutils.model

data class TorrentInfo(
    val title: String,
    val files: List<TorrentFile>,
    val hash: String,
    val size: Long,
    val trackers: List<String> = emptyList(),
)
