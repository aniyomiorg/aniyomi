package eu.kanade.tachiyomi.network.interceptor

import android.webkit.CookieManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logcat.LogPriority
import logcat.logcat
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import uy.kohesive.injekt.injectLazy
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class FlareSolverrInterceptor(private val preferences: NetworkPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val flareSolverTag = "FlareSolverrInterceptor"
        val originalRequest = chain.request()

        val originalResponse = chain.proceed(originalRequest)

        // Check if Cloudflare anti-bot is on
        if (!(originalResponse.code in ERROR_CODES && originalResponse.header("Server") in SERVER_CHECK)) {
            return originalResponse
        }

        // FlareSolverr is disabled, so just proceed with the request.
        if (!preferences.enableFlareSolverr().get()) {
            return chain.proceed(originalRequest)
        }

        logcat(LogPriority.INFO, flareSolverTag, { "Intercepting request: ${originalRequest.url}" })

        return try {
            originalResponse.close()

            val request =
                runBlocking {
                    CFClearance.resolveWithFlareSolverr(originalRequest)
                }

            chain.proceed(request)
        } catch (e: Exception) {
            // Because OkHttp's enqueue only handles IOExceptions, wrap the exception so that
            // we don't crash the entire app
            throw IOException(e)
        }
    }

    object CFClearance {
        private val network: NetworkHelper by injectLazy()
        private val json: Json by injectLazy()
        private val jsonMediaType = "application/json".toMediaType()
        private val networkPreferences: NetworkPreferences by injectLazy()
        private val flareSolverrUrl = networkPreferences.flareSolverrUrl().get()
        private val mutex = Mutex()

        @Serializable
        data class FlareSolverCookie(
            val name: String,
            val value: String,
        )

        @Serializable
        data class FlareSolverRequest(
            val cmd: String,
            val url: String,
            val maxTimeout: Int? = null,
            val session: List<String>? = null,
            @SerialName("session_ttl_minutes")
            val sessionTtlMinutes: Int? = null,
            val cookies: List<FlareSolverCookie>? = null,
            val returnOnlyCookies: Boolean? = null,
            val proxy: String? = null,
            val postData: String? = null, // only used with cmd 'request.post'
        )

        @Serializable
        data class FlareSolverSolutionCookie(
            val name: String,
            val value: String,
            val domain: String,
            val path: String,
            val expires: Double? = null,
            val size: Int? = null,
            val httpOnly: Boolean,
            val secure: Boolean,
            val session: Boolean? = null,
            val sameSite: String,
        )

        @Serializable
        data class FlareSolverSolution(
            val url: String,
            val status: Int,
            val headers: Map<String, String>? = null,
            val response: String? = null,
            val cookies: List<FlareSolverSolutionCookie>,
            val userAgent: String,
        )

        @Serializable
        data class FlareSolverResponse(
            val solution: FlareSolverSolution,
            val status: String,
            val message: String,
            val startTimestamp: Long,
            val endTimestamp: Long,
            val version: String,
        )

        suspend fun resolveWithFlareSolverr(
            originalRequest: Request,
            cookieManager: CookieManager = CookieManager.getInstance(),
        ): Request {
            val flareSolverTag = "FlareSolverr"

            logcat(LogPriority.INFO, flareSolverTag, { "Requesting challenge solution for ${originalRequest.url}" })

            val flareSolverResponse =
                with(json) {
                    mutex.withLock {
                        network.client.newCall(
                            POST(
                                url = flareSolverrUrl,
                                body =
                                Json.encodeToString(
                                    FlareSolverRequest(
                                        "request.get",
                                        originalRequest.url.toString(),
                                        cookies =
                                        network.cookieJar.get(originalRequest.url).map {
                                            FlareSolverCookie(it.name, it.value)
                                        },
                                        returnOnlyCookies = true,
                                        maxTimeout = 30000,
                                    ),
                                ).toRequestBody(jsonMediaType),
                            ),
                        ).awaitSuccess().parseAs<FlareSolverResponse>()
                    }
                }

            if (flareSolverResponse.solution.status in 200..299) {
                logcat(
                    LogPriority.INFO,
                    flareSolverTag,
                    { "Received challenge solution for ${originalRequest.url}" },
                )
                logcat(
                    LogPriority.INFO,
                    flareSolverTag,
                    {
                        "Received cookies from FlareSolverr\n" +
                            flareSolverResponse.solution.cookies.joinToString("; ")
                    },
                )

                flareSolverResponse.solution.cookies.forEach { cookie ->
                    logcat(LogPriority.INFO, flareSolverTag, { "Creating cookie for ${cookie.name}" })
                    try {
                        val domain = cookie.domain.removePrefix(".")
                        val cookieString = buildCookieString(cookie, domain)
                        logcat(
                            LogPriority.INFO,
                            flareSolverTag,
                            { "Adding cookie string to CookieManager: $cookieString" },
                        )
                        cookieManager.setCookie("https://$domain", cookieString)
                    } catch (e: Exception) {
                        logcat(LogPriority.ERROR, flareSolverTag, { "Error creating cookie for ${cookie.name} $e" })
                        throw e
                    }
                }

                // Verify if the cookies are set correctly
                val allCookies = flareSolverResponse.solution.cookies.mapNotNull { cookie ->
                    val domain = cookie.domain.removePrefix(".")
                    val setCookie = cookieManager.getCookie("https://$domain")
                    logcat(LogPriority.INFO, flareSolverTag, { "Set cookies in CookieManager for $domain: $setCookie" })
                    setCookie
                }.joinToString("; ")

                logcat(
                    LogPriority.INFO,
                    flareSolverTag,
                    { "Final cookies\n$allCookies ans user agent: ${flareSolverResponse.solution.userAgent}" },
                )
                return originalRequest.newBuilder()
                    .header("Cookie", allCookies)
                    .header("User-Agent", flareSolverResponse.solution.userAgent)
                    .build()
            } else {
                logcat(
                    LogPriority.ERROR,
                    flareSolverTag,
                    { "Failed to solve challenge: ${flareSolverResponse.message}" },
                )
                throw CloudflareBypassException()
            }
        }

        private fun buildCookieString(cookie: FlareSolverSolutionCookie, domain: String): String {
            val formatter = DateTimeFormatter.RFC_1123_DATE_TIME
            val expires = if (cookie.expires != null && cookie.expires > 0) {
                ZonedDateTime.now().plusSeconds(cookie.expires.toLong()).format(formatter)
            } else {
                "Fri, 31 Dec 9999 23:59:59 GMT"
            }

            return StringBuilder().apply {
                append("${cookie.name}=${cookie.value}; Domain=$domain; Path=${cookie.path}; Expires=$expires;")
                if (cookie.httpOnly) append(" HttpOnly;")
                if (cookie.secure) append(" Secure;")
            }.toString()
        }

        private class CloudflareBypassException : Exception()
    }
}
