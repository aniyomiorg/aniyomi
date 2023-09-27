package aniyomi.util

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.source.service.SourcePreferences.DataSaver.BANDWIDTH_HERO
import eu.kanade.domain.source.service.SourcePreferences.DataSaver.NONE
import eu.kanade.domain.source.service.SourcePreferences.DataSaver.RESMUSH_IT
import eu.kanade.domain.source.service.SourcePreferences.DataSaver.WSRV_NL
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.MangaSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.OkHttpClient
import okhttp3.Response
import rx.Observable
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.injectLazy

interface DataSaver {

    fun compress(imageUrl: String): String

    companion object {
        val NoOp = object : DataSaver {
            override fun compress(imageUrl: String): String {
                return imageUrl
            }
        }

        fun HttpSource.fetchImage(page: Page, dataSaver: DataSaver): Observable<Response> {
            val imageUrl = page.imageUrl ?: return fetchImage(page)
            page.imageUrl = dataSaver.compress(imageUrl)
            return fetchImage(page)
                .doOnNext {
                    page.imageUrl = imageUrl
                }
        }

        suspend fun HttpSource.getImage(page: Page, dataSaver: DataSaver): Response {
            val imageUrl = page.imageUrl ?: return getImage(page)
            page.imageUrl = dataSaver.compress(imageUrl)
            return try {
                getImage(page)
            } finally {
                page.imageUrl = imageUrl
            }
        }
    }
}

fun DataSaver(source: MangaSource, preferences: SourcePreferences): DataSaver {
    val dataSaver = preferences.dataSaver().get()
    if (dataSaver != NONE && source.id.toString() in preferences.dataSaverExcludedSources().get()) {
        return DataSaver.NoOp
    }
    return when (dataSaver) {
        NONE -> DataSaver.NoOp
        BANDWIDTH_HERO -> BandwidthHeroDataSaver(preferences)
        WSRV_NL -> WsrvNlDataSaver(preferences)
        RESMUSH_IT -> ReSmushItDataSaver(preferences)
    }
}

private class BandwidthHeroDataSaver(preferences: SourcePreferences) : DataSaver {
    private val dataSavedServer = preferences.dataSaverServer().get().trimEnd('/')

    private val ignoreJpg = preferences.dataSaverIgnoreJpeg().get()
    private val ignoreGif = preferences.dataSaverIgnoreGif().get()

    private val format = preferences.dataSaverImageFormatJpeg().toIntRepresentation()
    private val quality = preferences.dataSaverImageQuality().get()
    private val colorBW = preferences.dataSaverColorBW().toIntRepresentation()

    override fun compress(imageUrl: String): String {
        return if (dataSavedServer.isNotBlank() && !imageUrl.contains(dataSavedServer)) {
            when {
                imageUrl.contains(".jpeg", true) || imageUrl.contains(".jpg", true) -> if (ignoreJpg) imageUrl else getUrl(imageUrl)
                imageUrl.contains(".gif", true) -> if (ignoreGif) imageUrl else getUrl(imageUrl)
                else -> getUrl(imageUrl)
            }
        } else {
            imageUrl
        }
    }

    private fun getUrl(imageUrl: String): String {
        // Network Request sent for the Bandwidth Hero Proxy server
        return "$dataSavedServer/?jpg=$format&l=$quality&bw=$colorBW&url=$imageUrl"
    }

    private fun Preference<Boolean>.toIntRepresentation() = if (get()) "1" else "0"
}

private class WsrvNlDataSaver(preferences: SourcePreferences) : DataSaver {
    private val ignoreJpg = preferences.dataSaverIgnoreJpeg().get()
    private val ignoreGif = preferences.dataSaverIgnoreGif().get()

    private val format = preferences.dataSaverImageFormatJpeg().get()
    private val quality = preferences.dataSaverImageQuality().get()

    override fun compress(imageUrl: String): String {
        return when {
            imageUrl.contains(".jpeg", true) || imageUrl.contains(".jpg", true) -> if (ignoreJpg) imageUrl else getUrl(imageUrl)
            imageUrl.contains(".gif", true) -> if (ignoreGif) imageUrl else getUrl(imageUrl)
            else -> getUrl(imageUrl)
        }
    }

    private fun getUrl(imageUrl: String): String {
        // Network Request sent to wsrv
        return "https://wsrv.nl/?url=$imageUrl" + if (imageUrl.contains(".webp", true) || imageUrl.contains(".gif", true)) {
            if (!format) {
                // Preserve output image extension for animated images(.webp and .gif)
                "&q=$quality&n=-1"
            } else {
                // Do not preserve output Extension if User asked to convert into Jpeg
                "&output=jpg&q=$quality&n=-1"
            }
        } else {
            if (format) {
                "&output=jpg&q=$quality"
            } else {
                "&output=webp&q=$quality"
            }
        }
    }
}

private class ReSmushItDataSaver(preferences: SourcePreferences) : DataSaver {

    private val network: NetworkHelper by injectLazy()

    private val client: OkHttpClient
        get() = network.client

    private val ignoreJpg = preferences.dataSaverIgnoreJpeg().get()
    private val ignoreGif = preferences.dataSaverIgnoreGif().get()

    private val quality = preferences.dataSaverImageQuality().get()

    override fun compress(imageUrl: String): String {
        return when {
            imageUrl.contains(".jpeg", true) || imageUrl.contains(".jpg", true) -> if (ignoreJpg) imageUrl else getUrl(imageUrl)
            imageUrl.contains(".gif", true) -> if (ignoreGif) imageUrl else getUrl(imageUrl)
            else -> getUrl(imageUrl)
        }
    }

    private fun getUrl(imageUrl: String): String {
        // Network Request sent to resmush
        return client.newCall(GET("http://api.resmush.it/ws.php?img=$imageUrl&qlty=$quality")).execute()
            .body.string().substringAfter("\"dest\":\"").substringBefore("\",")
    }
}
