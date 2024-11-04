package eu.kanade.tachiyomi.data.track.simkl

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.track.simkl.SimklApi.Companion.CLIENT_ID
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklOAuth
import okhttp3.Interceptor
import okhttp3.Response

class SimklInterceptor(val simkl: Simkl) : Interceptor {

    /**
     * OAuth object used for authenticated requests.
     */
    private var oauth: SimklOAuth? = simkl.restoreToken()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val oauth = oauth ?: throw Exception("Not authenticated with Simkl")

        // Add the authorization header to the original request.
        val authRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${oauth.accessToken}")
            .addHeader("simkl-api-key", CLIENT_ID)
            .header("User-Agent", "Aniyomi v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})")
            .build()

        return chain.proceed(authRequest)
    }

    fun newAuth(oauth: SimklOAuth?) {
        this.oauth = oauth
        simkl.saveToken(oauth)
    }
}
