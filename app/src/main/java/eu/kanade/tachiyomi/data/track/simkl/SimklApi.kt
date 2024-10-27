package eu.kanade.tachiyomi.data.track.simkl

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy

class SimklApi(private val client: OkHttpClient, interceptor: SimklInterceptor) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    suspend fun addLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val type = track.tracking_url
                .substringAfter("/")
                .substringBefore("/")
            val mediaType = if (type == "movies") "movies" else "shows"
            addToList(track, mediaType)

            track
        }
    }

    private suspend fun addToList(track: AnimeTrack, mediaType: String) {
        val payload = buildJsonObject {
            putJsonArray(mediaType) {
                addJsonObject {
                    putJsonObject("ids") {
                        put("simkl", track.remote_id)
                    }
                    put("to", track.toSimklStatus())
                }
            }
        }.toString().toRequestBody(jsonMime)
        authClient.newCall(
            POST("$API_URL/sync/add-to-list", body = payload),
        ).awaitSuccess()
    }

    private suspend fun updateRating(track: AnimeTrack, mediaType: String) {
        val payload = buildJsonObject {
            putJsonArray(mediaType) {
                addJsonObject {
                    putJsonObject("ids") {
                        put("simkl", track.remote_id)
                    }
                    put("rating", track.score.toInt())
                }
            }
        }.toString().toRequestBody(jsonMime)

        if (track.score == 0.0) {
            authClient.newCall(
                POST("$API_URL/sync/ratings/remove", body = payload),
            ).awaitSuccess()
        } else {
            authClient.newCall(
                POST("$API_URL/sync/ratings", body = payload),
            ).awaitSuccess()
        }
    }

    private suspend fun updateProgress(track: AnimeTrack) {
        // first remove
        authClient.newCall(
            POST("$API_URL/sync/history/remove", body = buildProgressObject(track, false)),
        ).awaitSuccess()
        // then add again
        authClient.newCall(
            POST("$API_URL/sync/history", body = buildProgressObject(track, true)),
        ).awaitSuccess()
    }

    private fun buildProgressObject(track: AnimeTrack, add: Boolean = true) = buildJsonObject {
        putJsonArray("shows") {
            addJsonObject {
                putJsonObject("ids") {
                    put("simkl", track.remote_id)
                }
                putJsonArray("seasons") {
                    addJsonObject {
                        put("number", 1)
                        if (add) {
                            putJsonArray("episodes") {
                                for (epNum in 1..track.last_episode_seen.toInt()) {
                                    addJsonObject {
                                        put("number", epNum)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }.toString().toRequestBody(jsonMime)

    suspend fun updateLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            // determine media type
            val type = track.tracking_url
                .substringAfter("/")
                .substringBefore("/")
            val mediaType = if (type == "movies") "movies" else "shows"
            // update progress only for shows
            if (type != "movies") {
                updateProgress(track)
            }
            // add to correct list
            addToList(track, mediaType)
            // update rating
            updateRating(track, mediaType)

            track
        }
    }

    suspend fun searchAnime(search: String, type: String): List<AnimeTrackSearch> {
        return withIOContext {
            val searchUrl = "$API_URL/search/$type".toUri().buildUpon()
                .appendQueryParameter("q", search)
                .appendQueryParameter("extended", "full")
                .appendQueryParameter("client_id", CLIENT_ID)
                .build()
            with(json) {
                client.newCall(GET(searchUrl.toString()))
                    .awaitSuccess()
                    .parseAs<JsonArray>()
                    .let { response ->
                        response.map {
                            jsonToAnimeSearch(it.jsonObject, type)
                        }
                    }
            }
        }
    }

    private fun jsonToAnimeSearch(obj: JsonObject, type: String): AnimeTrackSearch {
        return AnimeTrackSearch.create(TrackerManager.SIMKL).apply {
            remote_id = obj["ids"]!!.jsonObject["simkl_id"]!!.jsonPrimitive.long
            title = obj["title_romaji"]?.jsonPrimitive?.content ?: obj["title"]!!.jsonPrimitive.content
            total_episodes = obj["ep_count"]?.jsonPrimitive?.longOrNull ?: 1
            cover_url = "https://simkl.in/posters/" + obj["poster"]!!.jsonPrimitive.content + "_m.webp"
            summary = obj["all_titles"]?.jsonArray
                ?.joinToString("\n", "All titles:\n") { it.jsonPrimitive.content } ?: ""

            tracking_url = obj["url"]!!.jsonPrimitive.content
            publishing_status = obj["status"]?.jsonPrimitive?.content ?: "ended"
            publishing_type = obj["type"]?.jsonPrimitive?.content ?: type
            start_date = obj["year"]?.jsonPrimitive?.intOrNull?.toString() ?: ""
        }
    }

    private fun jsonToAnimeTrack(
        obj: JsonObject,
        typeName: String,
        type: String,
        statusString: String,
    ): AnimeTrack {
        return AnimeTrack.create(TrackerManager.SIMKL).apply {
            title = obj[typeName]!!.jsonObject["title"]!!.jsonPrimitive.content
            val id = obj[typeName]!!.jsonObject["ids"]!!.jsonObject["simkl"]!!.jsonPrimitive.long
            remote_id = id
            if (typeName != "movie") {
                total_episodes =
                    obj["total_episodes_count"]!!
                        .jsonPrimitive.long
                last_episode_seen =
                    obj["watched_episodes_count"]!!
                        .jsonPrimitive.double
            } else {
                total_episodes = 1
                last_episode_seen = if (statusString == "completed") 1.0 else 0.0
            }
            score = obj["user_rating"]!!.jsonPrimitive.intOrNull?.toDouble() ?: 0.0
            status = toTrackStatus(statusString)
            tracking_url = "/$type/$id"
        }
    }

    /**
     * Checks if the given [track] exists in the user's list and
     * returns all info about it or null if it isn't found.
     */
    suspend fun findLibAnime(track: AnimeTrack): AnimeTrack? {
        return withIOContext {
            val payload = buildJsonArray {
                addJsonObject {
                    put("simkl", track.remote_id)
                }
            }.toString().toRequestBody(jsonMime)
            val foundAnime =
                with(json) {
                    authClient.newCall(
                        POST("$API_URL/sync/watched", body = payload),
                    )
                        .awaitSuccess()
                        .parseAs<JsonArray>()
                        .firstOrNull()?.jsonObject ?: return@withIOContext null
                }

            if (foundAnime["result"]?.jsonPrimitive?.booleanOrNull != true) return@withIOContext null
            val lastWatched = foundAnime["last_watched"]?.jsonPrimitive?.contentOrNull ?: return@withIOContext null
            val status = foundAnime["list"]!!.jsonPrimitive.content
            val type = track.tracking_url
                .substringAfter("/")
                .substringBefore("/")
            val queryType = if (type == "tv") "shows" else type
            val url = "$API_URL/sync/all-items/$queryType/$status".toUri().buildUpon()
                .appendQueryParameter("date_from", lastWatched)
                .build()

            val typeName = if (type == "movies") "movie" else "show"
            val listAnime =
                with(json) {
                    authClient.newCall(GET(url.toString()))
                        .awaitSuccess()
                        .parseAs<JsonObject>()[queryType]!!.jsonArray
                        .firstOrNull {
                            it.jsonObject[typeName]
                                ?.jsonObject?.get("ids")
                                ?.jsonObject?.get("simkl")
                                ?.jsonPrimitive?.long == track.remote_id
                        }?.jsonObject ?: return@withIOContext null
                }
            jsonToAnimeTrack(listAnime, typeName, type, status)
        }
    }

    fun getCurrentUser(): Int {
        return runBlocking {
            with(json) {
                authClient.newCall(GET("$API_URL/users/settings"))
                    .awaitSuccess()
                    .parseAs<JsonObject>()
                    .let {
                        it["account"]!!.jsonObject["id"]!!.jsonPrimitive.int
                    }
            }
        }
    }

    suspend fun accessToken(code: String): OAuth {
        return withIOContext {
            with(json) {
                client.newCall(accessTokenRequest(code))
                    .awaitSuccess()
                    .parseAs()
            }
        }
    }

    private fun accessTokenRequest(code: String) = POST(
        OAUTH_URL,
        body = buildJsonObject {
            put("code", code)
            put("client_id", CLIENT_ID)
            put("client_secret", CLIENT_SECRET)
            put("redirect_uri", REDIRECT_URL)
            put("grant_type", "authorization_code")
        }.toString().toRequestBody(jsonMime),
    )

    companion object {
        const val CLIENT_ID = "aa62a7da32518aae5d5049a658b87fa4837c3b739e06ed250b315aab6af82b0e"
        private const val CLIENT_SECRET = "2bec9c1d0c00a1e9b0e9e096a71f88d555a6f52da7923df07906df3b21351783"

        private const val BASE_URL = "https://simkl.com"
        private const val API_URL = "https://api.simkl.com"
        private const val OAUTH_URL = "$API_URL/oauth/token"
        private const val LOGIN_URL = "$BASE_URL/oauth/authorize"

        private const val REDIRECT_URL = "aniyomi://simkl-auth"

        fun authUrl(): Uri =
            LOGIN_URL.toUri().buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URL)
                .build()
    }
}
