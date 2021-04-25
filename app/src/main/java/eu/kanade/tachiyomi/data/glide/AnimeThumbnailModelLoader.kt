package eu.kanade.tachiyomi.data.glide

import com.bumptech.glide.integration.okhttp3.OkHttpStreamFetcher
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.InputStream

/**
 * A class for loading a cover associated with a [Anime] that can be present in our own cache.
 * Coupled with [AnimelibAnimeUrlFetcher], this class allows to implement the following flow:
 *
 * - Check in RAM LRU.
 * - Check in disk LRU.
 * - Check in this module.
 * - Fetch from the network connection.
 *
 * @param context the application context.
 */
class AnimeThumbnailModelLoader : ModelLoader<AnimeThumbnail, InputStream> {

    /**
     * Cover cache where persistent covers are stored.
     */
    private val coverCache: AnimeCoverCache by injectLazy()

    /**
     * Source manager.
     */
    private val sourceManager: SourceManager by injectLazy()

    /**
     * Default network client.
     */
    private val defaultClient = Injekt.get<NetworkHelper>().client

    /**
     * Map where request headers are stored for a source.
     */
    private val cachedHeaders = hashMapOf<Long, LazyHeaders>()

    /**
     * Factory class for creating [AnimeThumbnailModelLoader] instances.
     */
    class Factory : ModelLoaderFactory<AnimeThumbnail, InputStream> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<AnimeThumbnail, InputStream> {
            return AnimeThumbnailModelLoader()
        }

        override fun teardown() {}
    }

    override fun handles(model: AnimeThumbnail): Boolean {
        return true
    }

    /**
     * Returns a fetcher for the given anime or null if the url is empty.
     *
     * @param animeThumbnail the model.
     * @param width the width of the view where the resource will be loaded.
     * @param height the height of the view where the resource will be loaded.
     */
    override fun buildLoadData(
        animeThumbnail: AnimeThumbnail,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream>? {
        val anime = animeThumbnail.anime
        val url = anime.thumbnail_url

        if (url.isNullOrEmpty()) {
            return if (!anime.favorite || anime.isLocal()) {
                null
            } else {
                ModelLoader.LoadData(animeThumbnail, AnimelibAnimeCustomCoverFetcher(anime, coverCache))
            }
        }

        if (url.startsWith("http", true)) {
            val source = sourceManager.get(anime.source) as? HttpSource
            val glideUrl = GlideUrl(url, getHeaders(anime, source))

            // Get the resource fetcher for this request url.
            val networkFetcher = OkHttpStreamFetcher(source?.client ?: defaultClient, glideUrl)

            if (!anime.favorite) {
                return ModelLoader.LoadData(glideUrl, networkFetcher)
            }

            val libraryFetcher = AnimelibAnimeUrlFetcher(networkFetcher, anime, coverCache)

            // Return an instance of the fetcher providing the needed elements.
            return ModelLoader.LoadData(animeThumbnail, libraryFetcher)
        } else {
            // Return an instance of the fetcher providing the needed elements.
            return ModelLoader.LoadData(animeThumbnail, FileFetcher(url.removePrefix("file://")))
        }
    }

    /**
     * Returns the request headers for a source copying its OkHttp headers and caching them.
     *
     * @param anime the model.
     */
    private fun getHeaders(anime: Anime, source: HttpSource?): Headers {
        if (source == null) return LazyHeaders.DEFAULT

        return cachedHeaders.getOrPut(anime.source) {
            LazyHeaders.Builder().apply {
                val nullStr: String? = null
                setHeader("User-Agent", nullStr)
                for ((key, value) in source.headers.toMultimap()) {
                    addHeader(key, value[0])
                }
            }.build()
        }
    }
}
