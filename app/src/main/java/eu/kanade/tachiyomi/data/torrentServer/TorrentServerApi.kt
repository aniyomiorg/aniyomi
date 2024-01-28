package eu.kanade.tachiyomi.data.torrentServer

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.torrentServer.model.Torrent
import eu.kanade.tachiyomi.data.torrentServer.model.TorrentRequest
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.injectLazy

object TorrentServerApi {
    private val network: NetworkHelper by injectLazy()
    private val hostUrl = TorrentServerUtils.hostUrl

    fun echo(): String {
        return try {
            network.client.newCall(GET("$hostUrl/echo")).execute().body.string()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) println(e.message)
            ""
        }
    }

    fun shutdown(): String {
        return try {
            network.client.newCall(GET("$hostUrl/shutdown")).execute().body.string()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) println(e.message)
            ""
        }
    }

    /// Torrents
    fun addTorrent(link: String, title: String, poster: String, data: String, save: Boolean): Torrent {
        val req = TorrentRequest("add", link = link, title = title, poster = poster, data = data, save_to_db = save,).toString()
        val resp = network.client.newCall(POST("$hostUrl/torrents",body = req.toRequestBody("application/json".toMediaTypeOrNull()))).execute()
        return Json.decodeFromString(Torrent.serializer(), resp.body.string())
    }

    fun getTorrent(hash: String): Torrent {
        val req = TorrentRequest("get", hash,).toString()
        val resp = network.client.newCall(POST("$hostUrl/torrents",body = req.toRequestBody("application/json".toMediaTypeOrNull()))).execute()
        return Json.decodeFromString(Torrent.serializer(), resp.body.string())
    }

    fun remTorrent(hash: String) {
        val req = TorrentRequest("rem", hash,).toString()
        network.client.newCall(POST("$hostUrl/torrents",body = req.toRequestBody("application/json".toMediaTypeOrNull()))).execute()
    }


    fun listTorrent(): List<Torrent> {
        val req = TorrentRequest("list",).toString()
        val resp = network.client.newCall(POST("$hostUrl/torrents",body = req.toRequestBody("application/json".toMediaTypeOrNull()))).execute()
        return Json.decodeFromString<List<Torrent>>(resp.body.string())
    }


}
