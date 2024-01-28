package eu.kanade.tachiyomi.data.torrentServer

import android.app.Application
import android.os.Build
import eu.kanade.tachiyomi.data.torrentServer.model.TorrServVersion
import eu.kanade.tachiyomi.data.torrentServer.service.TorrentServerService
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import eu.kanade.tachiyomi.util.storage.saveTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.IOException

object UpdateTorrentServer {
    private const val UPDATE_SERVER_PATH = "https://raw.githubusercontent.com/Diegopyl1209/TorrServer/master/server_release.json"
    private val serverFile = TorrentServerFile()
    private val network: NetworkHelper by injectLazy()
    private val applicationContext: Application by injectLazy()
    private var version: TorrServVersion? = null

    suspend fun getLocalVersion(): String {
        var lv: String?
        withContext(Dispatchers.IO) {
            lv = TorrentServerApi.echo()
        }
        return lv ?: ""
    }

    suspend fun updateFromNet(onProgress: ((prc: Int) -> Unit)?) {
        val url = getLink()
        val progressListener =
            object : ProgressListener {
                // Progress of the download
                var savedProgress = 0

                override fun update(
                    bytesRead: Long,
                    contentLength: Long,
                    done: Boolean,
                ) {
                    val prc = (bytesRead * 100 / contentLength).toInt()
                    if (prc > savedProgress) {
                        savedProgress = prc
                        onProgress?.invoke(prc)
                    }
                }
            }
        if (url.isNotBlank()) {
            // download the file
            val response =
                network.client.newCachelessCallWithProgress(GET(url), progressListener)
                    .await()

            val updateFile = File(applicationContext.filesDir, "torrserver_update")

            if (response.isSuccessful) {
                response.body.source().saveTo(updateFile)
            } else {
                response.close()
                throw Exception("Unsuccessful response")
            }
            if (!updateFile.renameTo(serverFile)) {
                updateFile.delete()
                throw IOException("error write torrserver update")
            }

            if (!serverFile.setExecutable(true)) {
                serverFile.delete()
                throw IOException("error set exec permission")
            }
        }
        TorrentServerService.start()
    }

    private fun getArch(): String {
        when (Build.SUPPORTED_ABIS[0]) {
            "arm64-v8a" -> return "arm64"
            "armeabi-v7a" -> return "arm7"
            "x86_64" -> return "amd64"
            "x86" -> return "386"
        }
        return ""
    }

    private fun check(): Boolean {
        return try {
            val body = network.client.newCall(GET(UPDATE_SERVER_PATH)).execute().body.string()
            version = Json.decodeFromString(TorrServVersion.serializer(), body)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getLink(): String {
        if (version == null) {
            check()
        }
        if (version == null) {
            return ""
        }
        version?.let { ver ->
            val arch = getArch()
            if (arch.isEmpty()) {
                throw IOException("error get arch")
            }
            return ver.links["android-$arch"] ?: ""
        }
        return ""
    }
}
