package eu.kanade.tachiyomi.data.track.shikimori

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.data.track.model.TrackAnimeMetadata
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMAddEntryResponse
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMEntry
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMMetadata
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMOAuth
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMUser
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMUserListEntry
import eu.kanade.tachiyomi.network.DELETE
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack
import tachiyomi.domain.track.manga.model.MangaTrack as DomainMangaTrack

class ShikimoriApi(
    private val trackId: Long,
    private val client: OkHttpClient,
    interceptor: ShikimoriInterceptor,
) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    suspend fun addLibManga(track: MangaTrack, userId: String): MangaTrack {
        return withIOContext {
            with(json) {
                val payload = buildJsonObject {
                    putJsonObject("user_rate") {
                        put("user_id", userId)
                        put("target_id", track.remote_id)
                        put("target_type", "Manga")
                        put("chapters", track.last_chapter_read.toInt())
                        put("score", track.score.toInt())
                        put("status", track.toShikimoriStatus())
                    }
                }
                authClient.newCall(
                    POST(
                        "$API_URL/v2/user_rates",
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                ).awaitSuccess()
                    .parseAs<SMAddEntryResponse>()
                    .let {
                        track.library_id = it.id
                    }
                track
            }
        }
    }

    suspend fun updateLibManga(track: MangaTrack, userId: String): MangaTrack = addLibManga(
        track,
        userId,
    )

    suspend fun deleteLibManga(track: DomainMangaTrack) {
        withIOContext {
            authClient
                .newCall(DELETE("$API_URL/v2/user_rates/${track.libraryId}"))
                .awaitSuccess()
        }
    }

    suspend fun addLibAnime(track: AnimeTrack, userId: String): AnimeTrack {
        return withIOContext {
            with(json) {
                val payload = buildJsonObject {
                    putJsonObject("user_rate") {
                        put("user_id", userId)
                        put("target_id", track.remote_id)
                        put("target_type", "Anime")
                        put("episodes", track.last_episode_seen.toInt())
                        put("score", track.score.toInt())
                        put("status", track.toShikimoriStatus())
                    }
                }
                authClient.newCall(
                    POST(
                        "$API_URL/v2/user_rates",
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                ).awaitSuccess()
                    .parseAs<SMAddEntryResponse>()
                    .let {
                        track.library_id = it.id
                    }
                track
            }
        }
    }

    suspend fun updateLibAnime(track: AnimeTrack, userId: String): AnimeTrack = addLibAnime(
        track,
        userId,
    )

    suspend fun deleteLibAnime(track: DomainAnimeTrack) {
        withIOContext {
            authClient
                .newCall(DELETE("$API_URL/v2/user_rates/${track.libraryId}"))
                .awaitSuccess()
        }
    }

    suspend fun search(search: String): List<MangaTrackSearch> {
        return withIOContext {
            val url = "$API_URL/mangas".toUri().buildUpon()
                .appendQueryParameter("order", "popularity")
                .appendQueryParameter("search", search)
                .appendQueryParameter("limit", "20")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<List<SMEntry>>()
                    .map { it.toMangaTrack(trackId) }
            }
        }
    }

    suspend fun searchAnime(search: String): List<AnimeTrackSearch> {
        return withIOContext {
            val url = "$API_URL/animes".toUri().buildUpon()
                .appendQueryParameter("order", "popularity")
                .appendQueryParameter("search", search)
                .appendQueryParameter("limit", "20")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<List<SMEntry>>()
                    .map { it.toAnimeTrack(trackId) }
            }
        }
    }

    suspend fun findLibManga(track: MangaTrack, userId: String): MangaTrack? {
        return withIOContext {
            val urlMangas = "$API_URL/mangas".toUri().buildUpon()
                .appendPath(track.remote_id.toString())
                .build()
            val manga = with(json) {
                authClient.newCall(GET(urlMangas.toString()))
                    .awaitSuccess()
                    .parseAs<SMEntry>()
            }

            val url = "$API_URL/v2/user_rates".toUri().buildUpon()
                .appendQueryParameter("user_id", userId)
                .appendQueryParameter("target_id", track.remote_id.toString())
                .appendQueryParameter("target_type", "Manga")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<List<SMUserListEntry>>()
                    .let { entries ->
                        if (entries.size > 1) {
                            throw Exception("Too many manga in response")
                        }
                        entries
                            .map { it.toMangaTrack(trackId, manga) }
                            .firstOrNull()
                    }
            }
        }
    }

    suspend fun findLibAnime(track: AnimeTrack, user_id: String): AnimeTrack? {
        return withIOContext {
            val urlAnimes = "$API_URL/animes".toUri().buildUpon()
                .appendPath(track.remote_id.toString())
                .build()
            val anime = with(json) {
                authClient.newCall(GET(urlAnimes.toString()))
                    .awaitSuccess()
                    .parseAs<SMEntry>()
            }

            val url = "$API_URL/v2/user_rates".toUri().buildUpon()
                .appendQueryParameter("user_id", user_id)
                .appendQueryParameter("target_id", track.remote_id.toString())
                .appendQueryParameter("target_type", "Anime")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<List<SMUserListEntry>>()
                    .let { entries ->
                        if (entries.size > 1) {
                            throw Exception("Too many manga in response")
                        }
                        entries
                            .map { it.toAnimeTrack(trackId, anime) }
                            .firstOrNull()
                    }
            }
        }
    }

    suspend fun getCurrentUser(): Int {
        return with(json) {
            authClient.newCall(GET("$API_URL/users/whoami"))
                .awaitSuccess()
                .parseAs<SMUser>()
                .id
        }
    }

    suspend fun getMangaMetadata(track: DomainMangaTrack): TrackMangaMetadata {
        return withIOContext {
            val query = """
                |query(${'$'}ids: String!) {
                    |mangas(ids: ${'$'}ids) {
                        |id
                        |name
                        |description
                        |poster {
                            |originalUrl
                        |}
                        |personRoles {
                            |person {
                                |name
                            |}
                            |rolesEn
                        |}
                    |}
                |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("ids", "${track.remoteId}")
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        "https://shikimori.one/api/graphql",
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMMetadata>()
                    .let {
                        if (it.data.mangas.isEmpty()) throw Exception("Could not get metadata from Shikimori")
                        val manga = it.data.mangas[0]
                        TrackMangaMetadata(
                            remoteId = manga.id.toLong(),
                            title = manga.name,
                            thumbnailUrl = manga.poster.originalUrl,
                            description = manga.description,
                            authors = manga.personRoles
                                .filter { it.rolesEn.contains("Story") || it.rolesEn.contains("Story & Art") }
                                .map { it.person.name }
                                .joinToString(", ")
                                .ifEmpty { null },
                            artists = manga.personRoles
                                .filter { it.rolesEn.contains("Art") || it.rolesEn.contains("Story & Art") }
                                .map { it.person.name }
                                .joinToString(", ")
                                .ifEmpty { null },
                        )
                    }
            }
        }
    }

    suspend fun getAnimeMetadata(track: DomainAnimeTrack): TrackAnimeMetadata {
        return withIOContext {
            val query = """
                |query(${'$'}ids: String!) {
                    |animes(ids: ${'$'}ids) {
                        |id
                        |name
                        |description
                        |poster {
                            |originalUrl
                        |}
                        |personRoles {
                            |person {
                                |name
                            |}
                            |rolesEn
                        |}
                    |}
                |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("ids", "${track.remoteId}")
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        "https://shikimori.one/api/graphql",
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMMetadata>()
                    .let {
                        if (it.data.mangas.isEmpty()) throw Exception("Could not get metadata from Shikimori")
                        val anime = it.data.mangas[0]
                        TrackAnimeMetadata(
                            remoteId = anime.id.toLong(),
                            title = anime.name,
                            thumbnailUrl = anime.poster.originalUrl,
                            description = anime.description,
                            authors = anime.personRoles
                                .filter { it.rolesEn.contains("Story") || it.rolesEn.contains("Story & Art") }
                                .map { it.person.name }
                                .joinToString(", ")
                                .ifEmpty { null },
                            artists = anime.personRoles
                                .filter { it.rolesEn.contains("Art") || it.rolesEn.contains("Story & Art") }
                                .map { it.person.name }
                                .joinToString(", ")
                                .ifEmpty { null },
                        )
                    }
            }
        }
    }

    suspend fun accessToken(code: String): SMOAuth {
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
        const val BASE_URL = "https://shikimori.one"
        private const val API_URL = "$BASE_URL/api"
        private const val OAUTH_URL = "$BASE_URL/oauth/token"
        private const val LOGIN_URL = "$BASE_URL/oauth/authorize"

        private const val REDIRECT_URL = "animetail://shikimori-auth"

        private const val CLIENT_ID = "aOAYRqOLwxpA8skpcQIXetNy4cw2rn2fRzScawlcQ5U"
        private const val CLIENT_SECRET = "jqjmORn6bh2046ulkm4lHEwJ3OA1RmO3FD2sR9f6Clw"

        fun authUrl(): Uri = LOGIN_URL.toUri().buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URL)
            .appendQueryParameter("response_type", "code")
            .build()

        fun refreshTokenRequest(token: String) = POST(
            OAUTH_URL,
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("refresh_token", token)
                .build(),
        )
    }
}
