package eu.kanade.tachiyomi.data.track.jellyfin

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.sourcePreferences
import okhttp3.Interceptor
import okhttp3.Response
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.security.MessageDigest

class JellyfinInterceptor : Interceptor {

    private val sourceManager: AnimeSourceManager by injectLazy()

    private val apiKeys = mutableMapOf<String, String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Add the User-Agent header to the original request.
        val uaRequest = originalRequest.newBuilder()
            .header("User-Agent", "Aniyomi v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})")
            .build()

        // Check api keys
        if (originalRequest.url.queryParameter("api_key") != null) {
            return chain.proceed(uaRequest)
        }

        val userId = originalRequest.url.queryParameter("userId")
            ?: originalRequest.url.pathSegments
                .windowed(2)
                .firstOrNull { it[0] == "Users" }
                ?.get(1)
            ?: throw IOException("Please log in through the extension")
        val apiKey = apiKeys[userId] ?: getApiKey(userId)?.also { apiKeys[userId] = it }
            ?: throw IOException("Please log in through the extension")

        val authUrl = originalRequest.url.newBuilder()
            .addQueryParameter("api_key", apiKey)
            .build()

        val authRequest = uaRequest.newBuilder().url(authUrl).build()
        return chain.proceed(authRequest)
    }

    private fun getId(suffix: Int): Long {
        val key = "jellyfin" + (if (suffix == 1) "" else " ($suffix)") + "/all/$JELLYFIN_VERSION_ID"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }
            .reduce(Long::or) and Long.MAX_VALUE
    }

    private fun getApiKey(userId: String): String? {
        for (i in 1..MAX_JELLYFIN_SOURCES) {
            val sourceId = getId(i)
            val preferences = (sourceManager.get(sourceId) as ConfigurableAnimeSource).sourcePreferences()
            val sourceUserId = preferences.getString("user_id", "")

            if (sourceUserId.isNullOrEmpty()) {
                continue // Source not configured
            }

            if (sourceUserId == userId) {
                return preferences.getString("api_key", "")
            }
        }

        return null
    }

    companion object {
        private const val JELLYFIN_VERSION_ID = 1
        private const val MAX_JELLYFIN_SOURCES = 10
    }
}
