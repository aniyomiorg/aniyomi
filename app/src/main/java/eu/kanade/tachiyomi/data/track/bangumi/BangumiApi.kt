package eu.kanade.tachiyomi.data.track.bangumi

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMCollectionResponse
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMOAuth
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMSearchItem
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMSearchResult
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMSubject
import eu.kanade.tachiyomi.data.track.bangumi.dto.Infobox
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import tachiyomi.domain.track.manga.model.MangaTrack as DomainMangaTrack

class BangumiApi(
    private val trackId: Long,
    private val client: OkHttpClient,
    interceptor: BangumiInterceptor,
) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    suspend fun addLibManga(track: MangaTrack): MangaTrack {
        return withIOContext {
            val body = FormBody.Builder()
                .add("rating", track.score.toInt().toString())
                .add("status", track.toApiStatus())
                .build()
            authClient.newCall(POST("$API_URL/collection/${track.remote_id}/update", body = body))
                .awaitSuccess()
            track
        }
    }

    suspend fun addLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val body = FormBody.Builder()
                .add("rating", track.score.toInt().toString())
                .add("status", track.toApiStatus())
                .build()
            authClient.newCall(POST("$API_URL/collection/${track.remote_id}/update", body = body))
                .awaitSuccess()
            track
        }
    }

    suspend fun updateLibManga(track: MangaTrack): MangaTrack {
        return withIOContext {
            // read status update
            val sbody = FormBody.Builder()
                .add("rating", track.score.toInt().toString())
                .add("status", track.toApiStatus())
                .build()
            authClient.newCall(POST("$API_URL/collection/${track.remote_id}/update", body = sbody))
                .awaitSuccess()

            // chapter update
            val body = FormBody.Builder()
                .add("watched_eps", track.last_chapter_read.toInt().toString())
                .build()
            authClient.newCall(
                POST("$API_URL/subject/${track.remote_id}/update/watched_eps", body = body),
            ).awaitSuccess()

            track
        }
    }

    suspend fun updateLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            // read status update
            val sbody = FormBody.Builder()
                .add("rating", track.score.toInt().toString())
                .add("status", track.toApiStatus())
                .build()
            authClient.newCall(POST("$API_URL/collection/${track.remote_id}/update", body = sbody))
                .awaitSuccess()

            // chapter update
            val body = FormBody.Builder()
                .add("watched_eps", track.last_episode_seen.toInt().toString())
                .build()
            authClient.newCall(
                POST("$API_URL/subject/${track.remote_id}/update/watched_eps", body = body),
            ).awaitSuccess()

            track
        }
    }

    suspend fun search(search: String): List<MangaTrackSearch> {
        return withIOContext {
            val url = "$API_URL/search/subject/${URLEncoder.encode(
                search,
                StandardCharsets.UTF_8.name(),
            )}"
                .toUri()
                .buildUpon()
                .appendQueryParameter("type", "1")
                .appendQueryParameter("responseGroup", "large")
                .appendQueryParameter("max_results", "20")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<BGMSearchResult>()
                    .let { result ->
                        if (result.code == 404) emptyList<MangaTrackSearch>()

                        result.list
                            ?.map { it.toMangaTrackSearch(trackId) }
                            .orEmpty()
                    }
            }
        }
    }

    suspend fun searchAnime(search: String): List<AnimeTrackSearch> {
        return withIOContext {
            val url = "$API_URL/search/subject/${URLEncoder.encode(
                search,
                StandardCharsets.UTF_8.name(),
            )}"
                .toUri()
                .buildUpon()
                .appendQueryParameter("type", "2")
                .appendQueryParameter("responseGroup", "large")
                .appendQueryParameter("max_results", "20")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<BGMSearchResult>()
                    .let { result ->
                        if (result.code == 404) emptyList<AnimeTrackSearch>()

                        result.list
                            ?.map { it.toAnimeTrackSearch(trackId) }
                            .orEmpty()
                    }
            }
        }
    }

    suspend fun findLibManga(track: MangaTrack): MangaTrack? {
        return withIOContext {
            with(json) {
                authClient.newCall(GET("$API_URL/subject/${track.remote_id}"))
                    .awaitSuccess()
                    .parseAs<BGMSearchItem>()
                    .toMangaTrackSearch(trackId)
            }
        }
    }

    suspend fun findLibAnime(track: AnimeTrack): AnimeTrack? {
        return withIOContext {
            with(json) {
                authClient.newCall(GET("$API_URL/subject/${track.remote_id}"))
                    .awaitSuccess()
                    .parseAs<BGMSearchItem>()
                    .toAnimeTrackSearch(trackId)
            }
        }
    }

    suspend fun statusLibManga(track: MangaTrack): MangaTrack? {
        return withIOContext {
            val urlUserRead = "$API_URL/collection/${track.remote_id}"
            val requestUserRead = Request.Builder()
                .url(urlUserRead)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .get()
                .build()

            // TODO: get user readed chapter here
            with(json) {
                authClient.newCall(requestUserRead)
                    .awaitSuccess()
                    .parseAs<BGMCollectionResponse>()
                    .let {
                        if (it.code == 400) return@let null

                        track.status = it.status?.id!!
                        track.last_chapter_read = it.epStatus!!.toDouble()
                        track.score = it.rating!!
                        track
                    }
            }
        }
    }

    suspend fun statusLibAnime(track: AnimeTrack): AnimeTrack? {
        return withIOContext {
            val urlUserRead = "$API_URL/collection/${track.remote_id}"
            val requestUserRead = Request.Builder()
                .url(urlUserRead)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .get()
                .build()

            // TODO: get user readed chapter here
            with(json) {
                authClient.newCall(requestUserRead)
                    .awaitSuccess()
                    .parseAs<BGMCollectionResponse>()
                    .let {
                        if (it.code == 400) return@let null

                        track.status = it.status?.id!!
                        track.last_episode_seen = it.epStatus!!.toDouble()
                        track.score = it.rating!!
                        track
                    }
            }
        }
    }

    suspend fun getMangaMetadata(track: DomainMangaTrack): TrackMangaMetadata {
        return withIOContext {
            with(json) {
                authClient.newCall(GET("${API_URL}/v0/subjects/${track.remoteId}"))
                    .awaitSuccess()
                    .parseAs<BGMSubject>()
                    .let {
                        TrackMangaMetadata(
                            remoteId = it.id,
                            title = it.nameCn,
                            thumbnailUrl = it.images?.common,
                            description = it.summary,
                            authors = it.infobox
                                .filter { it.key == "作者" }
                                .filterIsInstance<Infobox.SingleValue>()
                                .map { it.value }
                                .joinToString(", "),
                            artists = it.infobox
                                .filter { it.key == "插图" }
                                .filterIsInstance<Infobox.SingleValue>()
                                .map { it.value }
                                .joinToString(", "),
                        )
                    }
            }
        }
    }

    suspend fun accessToken(code: String): BGMOAuth {
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
        body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .add("code", code)
            .add("redirect_uri", REDIRECT_URL)
            .build(),
    )

    companion object {
        private const val CLIENT_ID = "bgm293165b66d7e58156"
        private const val CLIENT_SECRET = "21d5f5c19ac24b4bc9c855ffa2387030"

        private const val API_URL = "https://api.bgm.tv"
        private const val OAUTH_URL = "https://bgm.tv/oauth/access_token"
        private const val LOGIN_URL = "https://bgm.tv/oauth/authorize"

        private const val REDIRECT_URL = "animetail://bangumi-auth"

        fun authUrl(): Uri =
            LOGIN_URL.toUri().buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", REDIRECT_URL)
                .build()

        fun refreshTokenRequest(token: String) = POST(
            OAUTH_URL,
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("refresh_token", token)
                .add("redirect_uri", REDIRECT_URL)
                .build(),
        )
    }
}
