package aniyomi.core.common.torrent.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TorrentRequest(
    val action: String,
    val hash: String = "",
    val link: String = "",
    val title: String = "",
    val poster: String = "",
    val data: String = "",
    @SerialName("save_to_db")
    val saveToDb: Boolean = false,
)
