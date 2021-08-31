package eu.kanade.tachiyomi.data.coil

import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.network.HttpException
import coil.request.get
import coil.size.Size
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.coil.AnimeCoverFetcher.Companion.USE_CUSTOM_COVER
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.buffer
import okio.sink
import okio.source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File

/**
 * Coil component that fetches [Anime] cover while using the cached file in disk when available.
 *
 * Available request parameter:
 * - [USE_CUSTOM_COVER]: Use custom cover if set by user, default is true
 */
class AnimeCoverFetcher : Fetcher<Anime> {
    private val coverCache: AnimeCoverCache by injectLazy()
    private val sourceManager: AnimeSourceManager by injectLazy()
    private val defaultClient = Injekt.get<NetworkHelper>().coilClient

    override fun key(data: Anime): String? {
        if (data.thumbnail_url.isNullOrBlank()) return null
        return data.thumbnail_url!!
    }

    override suspend fun fetch(pool: BitmapPool, data: Anime, size: Size, options: Options): FetchResult {
        // Use custom cover if exists
        val useCustomCover = options.parameters[USE_CUSTOM_COVER] as? Boolean ?: true
        val customCoverFile = coverCache.getCustomCoverFile(data)
        if (useCustomCover && customCoverFile.exists()) {
            return fileLoader(customCoverFile)
        }

        val cover = data.thumbnail_url
        return when (getResourceType(cover)) {
            Type.URL -> httpLoader(data, options)
            Type.File -> fileLoader(data)
            null -> error("Invalid image")
        }
    }

    private suspend fun httpLoader(anime: Anime, options: Options): FetchResult {
        // Only cache separately if it's a library item
        val coverCacheFile = if (anime.favorite) {
            coverCache.getCoverFile(anime) ?: error("No cover specified")
        } else {
            null
        }

        if (coverCacheFile?.exists() == true && options.diskCachePolicy.readEnabled) {
            return fileLoader(coverCacheFile)
        }

        val (response, body) = awaitGetCall(anime, options)
        if (!response.isSuccessful) {
            body.close()
            throw HttpException(response)
        }

        if (coverCacheFile != null && options.diskCachePolicy.writeEnabled) {
            @Suppress("BlockingMethodInNonBlockingContext")
            response.peekBody(Long.MAX_VALUE).source().use { input ->
                coverCacheFile.parentFile?.mkdirs()
                if (coverCacheFile.exists()) {
                    coverCacheFile.delete()
                }
                coverCacheFile.sink().buffer().use { output ->
                    output.writeAll(input)
                }
            }
        }

        return SourceResult(
            source = body.source(),
            mimeType = "image/*",
            dataSource = if (response.cacheResponse != null) DataSource.DISK else DataSource.NETWORK
        )
    }

    private suspend fun awaitGetCall(anime: Anime, options: Options): Pair<Response, ResponseBody> {
        val call = getCall(anime, options)
        val response = call.await()
        return response to checkNotNull(response.body) { "Null response source" }
    }

    private fun getCall(anime: Anime, options: Options): Call {
        val source = sourceManager.get(anime.source) as? AnimeHttpSource
        val request = Request.Builder().url(anime.thumbnail_url!!).also {
            if (source != null) {
                it.headers(source.headers)
            }

            val networkRead = options.networkCachePolicy.readEnabled
            val diskRead = options.diskCachePolicy.readEnabled
            when {
                !networkRead && diskRead -> {
                    it.cacheControl(CacheControl.FORCE_CACHE)
                }
                networkRead && !diskRead -> if (options.diskCachePolicy.writeEnabled) {
                    it.cacheControl(CacheControl.FORCE_NETWORK)
                } else {
                    it.cacheControl(CACHE_CONTROL_FORCE_NETWORK_NO_CACHE)
                }
                !networkRead && !diskRead -> {
                    // This causes the request to fail with a 504 Unsatisfiable Request.
                    it.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
                }
            }
        }.build()

        val client = source?.client?.newBuilder()?.cache(defaultClient.cache)?.build() ?: defaultClient
        return client.newCall(request)
    }

    private fun fileLoader(anime: Anime): FetchResult {
        return fileLoader(File(anime.thumbnail_url!!.substringAfter("file://")))
    }

    private fun fileLoader(file: File): FetchResult {
        return SourceResult(
            source = file.source().buffer(),
            mimeType = "image/*",
            dataSource = DataSource.DISK
        )
    }

    private fun getResourceType(cover: String?): Type? {
        return when {
            cover.isNullOrEmpty() -> null
            cover.startsWith("http", true) || cover.startsWith("Custom-", true) -> Type.URL
            cover.startsWith("/") || cover.startsWith("file://") -> Type.File
            else -> null
        }
    }

    private enum class Type {
        File, URL
    }

    companion object {
        const val USE_CUSTOM_COVER = "use_custom_cover"

        private val CACHE_CONTROL_FORCE_NETWORK_NO_CACHE = CacheControl.Builder().noCache().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()
    }
}
