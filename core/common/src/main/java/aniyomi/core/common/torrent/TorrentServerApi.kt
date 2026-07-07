package aniyomi.core.common.torrent

import aniyomi.core.common.torrent.model.Torrent
import aniyomi.core.common.torrent.model.TorrentRequest
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import logcat.LogPriority
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.system.logcat
import java.io.InputStream

class TorrentServerApi(
    private val network: NetworkHelper,
    private val json: Json,
) {
    val hostUrl
        get() = "http://127.0.0.1:$port"

    @Volatile
    private var port: Int = 0

    fun setPort(value: Int) {
        port = value
    }

    fun getPort(): Int {
        return port
    }

    suspend fun echo(): String {
        return try {
            network.client.newCall(GET("$hostUrl/echo")).awaitSuccess().body.string()
        } catch (e: Exception) {
            logcat(LogPriority.DEBUG, e) { "Error sending echo" }
            ""
        }
    }

    // / Torrents
    suspend fun addTorrent(
        link: String,
        title: String,
        poster: String = "",
        data: String = "",
        save: Boolean,
    ): Torrent {
        val req = json.encodeToString(
            TorrentRequest(
                "add",
                link = link,
                title = title,
                poster = poster,
                data = data,
                saveToDb = save,
            ),
        )
        val resp = network.client.newCall(
            POST(
                "$hostUrl/torrents",
                body = req.toRequestBody("application/json".toMediaTypeOrNull()),
            ),
        ).awaitSuccess()
        return resp.use { json.decodeFromStream<Torrent>(it.body.byteStream()) }
    }

    suspend fun uploadTorrent(
        file: InputStream,
        title: String,
        save: Boolean = false,
    ): Torrent {
        val bytes = file.use { it.readBytes() }
        val fileRequestBody = bytes.toRequestBody("application/x-bittorrent".toMediaTypeOrNull())

        val requestBody = MultipartBody.Builder().apply {
            setType(MultipartBody.FORM)
            addFormDataPart("file", title, fileRequestBody)
            addFormDataPart("save", save.toString())
            addFormDataPart("title", title)
        }.build()

        val resp = network.client.newCall(
            POST(
                "$hostUrl/torrent/upload",
                body = requestBody,
            ),
        ).awaitSuccess()

        return resp.use { json.decodeFromStream<Torrent>(it.body.byteStream()) }
    }
}
