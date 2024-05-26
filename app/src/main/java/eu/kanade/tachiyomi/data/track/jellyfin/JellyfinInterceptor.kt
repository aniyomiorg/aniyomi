package eu.kanade.tachiyomi.data.track.jellyfin

import eu.kanade.tachiyomi.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class JellyfinInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Add the User-Agent header to the original request.
        val uaRequest = originalRequest.newBuilder()
            .header("User-Agent", "Aniyomi v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})")
            .build()

        return chain.proceed(uaRequest)
    }
}
