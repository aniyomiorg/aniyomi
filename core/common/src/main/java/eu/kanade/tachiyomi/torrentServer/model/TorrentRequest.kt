package eu.kanade.tachiyomi.torrentServer.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@Serializable
data class TorrentRequest(
    val action: String,
    val hash: String = "",
    val link: String = "",
    val title: String = "",
    val poster: String = "",
    val data: String = "",
    val saveToDb: Boolean = false,
) {
    override fun toString(): String {
        return Json.encodeToString(serializer(), this)
    }
}

@Serializable
open class Request(val action: String) {
    override fun toString(): String {
        return Json.encodeToString(serializer(), this)
    }
}

class SettingsReq(
    action: String,
    val sets: BTSets,
) : Request(action)

class ViewedReq(
    action: String,
    val hash: String = "",
    val fileIndex: Int = -1,
) : Request(action)

data class Viewed(
    val hash: String,
    val fileIndex: Int,
)
