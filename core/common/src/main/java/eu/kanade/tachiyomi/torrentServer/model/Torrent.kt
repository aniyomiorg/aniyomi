package eu.kanade.tachiyomi.torrentServer.model

import kotlinx.serialization.Serializable

@Serializable
@Suppress("ConstructorParameterNaming")
data class Torrent(
    var title: String,
    var poster: String? = null,
    var data: String? = null,
    var timestamp: Long? = null,
    var name: String? = null,
    var hash: String? = null,
    var stat: Int? = null,
    var stat_string: String? = null,
    var loaded_size: Long? = null,
    var torrent_size: Long? = null,
    var preloaded_bytes: Long? = null,
    var preload_size: Long? = null,
    var download_speed: Double? = null,
    var upload_speed: Double? = null,
    var total_peers: Int? = null,
    var pending_peers: Int? = null,
    var active_peers: Int? = null,
    var connected_seeders: Int? = null,
    var half_open_peers: Int? = null,
    var bytes_written: Long? = null,
    var bytes_written_data: Long? = null,
    var bytes_read: Long? = null,
    var bytes_read_data: Long? = null,
    var bytes_read_useful_data: Long? = null,
    var chunks_written: Long? = null,
    var chunks_read: Long? = null,
    var chunks_read_useful: Long? = null,
    var chunks_read_wasted: Long? = null,
    var pieces_dirtied_good: Long? = null,
    var pieces_dirtied_bad: Long? = null,
    var duration_seconds: Double? = null,
    var bit_rate: String? = null,
    var file_stats: List<FileStat>? = null,
    var trackers: List<String>? = null,
)

@Serializable
data class FileStat(
    var id: Int? = null,
    var path: String,
    var length: Long,
)
