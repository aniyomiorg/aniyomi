package aniyomi.core.common.torrent.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Torrent(
    val title: String,
    val hash: String? = null,
    @SerialName("torrent_size")
    val torrentSize: Long? = null,
    val trackers: List<String>? = null,
    @SerialName("file_stats")
    val fileStats: List<FileStats>? = null,
)

@Serializable
data class FileStats(
    val id: Int? = null,
    val path: String,
    val length: Long,
)
