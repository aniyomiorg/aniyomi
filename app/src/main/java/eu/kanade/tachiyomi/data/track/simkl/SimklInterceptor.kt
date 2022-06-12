package eu.kanade.tachiyomi.data.track.simkl

import eu.kanade.tachiyomi.data.track.simkl.SimklApi.Companion.clientId
import okhttp3.Interceptor
import okhttp3.Response

class SimklInterceptor(val simkl: Simkl) : Interceptor {

    /**
     * OAuth object used for authenticated requests.
     */
    private var oauth: OAuth? = simkl.restoreToken()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val oauth = oauth ?: throw Exception("Not authenticated with Simkl")

        // Add the authorization header to the original request.
        val authRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${oauth.access_token}")
            .addHeader("simkl-api-key", clientId)
            .header("User-Agent", "Aniyomi")
            .build()

        return chain.proceed(authRequest)
    }

    fun newAuth(oauth: OAuth?) {
        this.oauth = oauth
        simkl.saveToken(oauth)
    }
}
