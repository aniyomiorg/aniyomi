package eu.kanade.tachiyomi.data.track.anilist

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.anilist.dto.ALAddEntryResult
import eu.kanade.tachiyomi.data.track.anilist.dto.ALCurrentUserResult
import eu.kanade.tachiyomi.data.track.anilist.dto.ALOAuth
import eu.kanade.tachiyomi.data.track.anilist.dto.ALSearchResult
import eu.kanade.tachiyomi.data.track.anilist.dto.ALUserListEntryQueryResult
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.minutes
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack
import tachiyomi.domain.track.manga.model.MangaTrack as DomainMangaTrack

class AnilistApi(val client: OkHttpClient, interceptor: AnilistInterceptor) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder()
        .addInterceptor(interceptor)
        .rateLimit(permits = 85, period = 1.minutes)
        .build()

    suspend fun addLibManga(track: MangaTrack): MangaTrack {
        return withIOContext {
            val query = """
            |mutation AddManga(${'$'}mangaId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}private: Boolean) {
                |SaveMediaListEntry (mediaId: ${'$'}mangaId, progress: ${'$'}progress, status: ${'$'}status, private: ${'$'}private) {
                |   id
                |   status
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("mangaId", track.remote_id)
                    put("progress", track.last_chapter_read.toInt())
                    put("status", track.toApiStatus())
                    put("private", track.private)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALAddEntryResult>()
                    .let {
                        track
                    }
            }
        }
    }

    suspend fun updateLibManga(track: MangaTrack): MangaTrack {
        return withIOContext {
            val query = """
            |mutation UpdateManga(
                |${'$'}listId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}private: Boolean,
                |${'$'}score: Int, ${'$'}startedAt: FuzzyDateInput, ${'$'}completedAt: FuzzyDateInput
            |) {
                |SaveMediaListEntry(
                    |id: ${'$'}listId, progress: ${'$'}progress, status: ${'$'}status, private: ${'$'}private,
                    |scoreRaw: ${'$'}score, startedAt: ${'$'}startedAt, completedAt: ${'$'}completedAt
                |) {
                    |id
                    |status
                    |progress
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("listId", track.library_id)
                    put("progress", track.last_chapter_read.toInt())
                    put("status", track.toApiStatus())
                    put("score", track.score.toInt())
                    put("startedAt", createDate(track.started_reading_date))
                    put("completedAt", createDate(track.finished_reading_date))
                    put("private", track.private)
                }
            }
            authClient.newCall(POST(API_URL, body = payload.toString().toRequestBody(jsonMime)))
                .awaitSuccess()
            track
        }
    }

    suspend fun deleteLibManga(track: DomainMangaTrack) {
        withIOContext {
            val query = """
            |mutation DeleteManga(${'$'}listId: Int) {
                |DeleteMediaListEntry(id: ${'$'}listId) {
                    |deleted
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("listId", track.libraryId)
                }
            }
            authClient.newCall(POST(API_URL, body = payload.toString().toRequestBody(jsonMime)))
                .awaitSuccess()
        }
    }

    suspend fun addLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val query = """
            |mutation AddAnime(${'$'}animeId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}private: Boolean) {
                |SaveMediaListEntry (mediaId: ${'$'}animeId, progress: ${'$'}progress, status: ${'$'}status, private: ${'$'}private) {
                |   id
                |   status
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("animeId", track.remote_id)
                    put("progress", track.last_episode_seen.toInt())
                    put("status", track.toApiStatus())
                    put("private", track.private)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALAddEntryResult>()
                    .let {
                        track
                    }
            }
        }
    }

    suspend fun updateLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val query = """
            |mutation UpdateAnime(
                |${'$'}listId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}private: Boolean,
                |${'$'}score: Int, ${'$'}startedAt: FuzzyDateInput, ${'$'}completedAt: FuzzyDateInput
            |) {
                |SaveMediaListEntry(
                    |id: ${'$'}listId, progress: ${'$'}progress, status: ${'$'}status, private: ${'$'}private,
                    |scoreRaw: ${'$'}score, startedAt: ${'$'}startedAt, completedAt: ${'$'}completedAt
                |) {
                    |id
                    |status
                    |progress
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("listId", track.library_id)
                    put("progress", track.last_episode_seen.toInt())
                    put("status", track.toApiStatus())
                    put("score", track.score.toInt())
                    put("startedAt", createDate(track.started_watching_date))
                    put("completedAt", createDate(track.finished_watching_date))
                    put("private", track.private)
                }
            }
            authClient.newCall(POST(API_URL, body = payload.toString().toRequestBody(jsonMime)))
                .awaitSuccess()
            track
        }
    }

    suspend fun deleteLibAnime(track: DomainAnimeTrack) {
        return withIOContext {
            val query = """
            |mutation DeleteAnime(${'$'}listId: Int) {
                |DeleteMediaListEntry(id: ${'$'}listId) {
                    |deleted
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("listId", track.libraryId)
                }
            }
            authClient.newCall(POST(API_URL, body = payload.toString().toRequestBody(jsonMime)))
                .awaitSuccess()
        }
    }

    suspend fun search(search: String): List<MangaTrackSearch> {
        return withIOContext {
            val query = """
            |query Search(${'$'}query: String) {
                |Page (perPage: 50) {
                    |media(search: ${'$'}query, type: MANGA, format_not_in: [NOVEL]) {
                        |id
                        |staff {
                            |edges {
                                |role
                                |id
                                |node {
                                    |name {
                                        |full
                                        |userPreferred
                                        |native
                                    |}
                                |}
                            |}
                        |}
                        |title {
                            |userPreferred
                        |}
                        |coverImage {
                            |large
                        |}
                        |format
                        |status
                        |chapters
                        |description
                        |startDate {
                            |year
                            |month
                            |day
                        |}
                        |averageScore
                    |}
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("query", search)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALSearchResult>()
                    .data.page.media
                    .map { it.toALManga().toTrack() }
            }
        }
    }

    suspend fun searchAnime(search: String): List<AnimeTrackSearch> {
        return withIOContext {
            val query = """
            |query Search(${'$'}query: String) {
                |Page (perPage: 50) {
                    |media(search: ${'$'}query, type: ANIME) {
                        |id
                        |studios {
                            |edges {
                                |isMain
                                |node {
                                    |name
                                |}
                            |}
                        |}
                        |title {
                            |userPreferred
                        |}
                        |coverImage {
                            |large
                        |}
                        |format
                        |status
                        |episodes
                        |description
                        |startDate {
                            |year
                            |month
                            |day
                        |}
                        |averageScore
                    |}
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("query", search)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALSearchResult>()
                    .data.page.media
                    .map { it.toALAnime().toTrack() }
            }
        }
    }

    suspend fun findLibManga(track: MangaTrack, userid: Int): MangaTrack? {
        return withIOContext {
            val query = """
            |query (${'$'}id: Int!, ${'$'}manga_id: Int!) {
                |Page {
                    |mediaList(userId: ${'$'}id, type: MANGA, mediaId: ${'$'}manga_id) {
                        |id
                        |status
                        |scoreRaw: score(format: POINT_100)
                        |progress
                        |private
                        |startedAt {
                            |year
                            |month
                            |day
                        |}
                        |completedAt {
                            |year
                            |month
                            |day
                        |}
                        |media {
                            |id
                            |title {
                                |userPreferred
                            |}
                            |coverImage {
                                |large
                            |}
                            |format
                            |status
                            |chapters
                            |description
                            |startDate {
                                |year
                                |month
                                |day
                            |}
                            |staff {
                                |edges {
                                    |role
                                    |id
                                    |node {
                                        |name {
                                            |full
                                            |userPreferred
                                            |native
                                        |}
                                    |}
                                |}
                            |}
                        |}
                    |}
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("id", userid)
                    put("manga_id", track.remote_id)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALUserListEntryQueryResult>()
                    .data.page.mediaList
                    .map { it.toALUserManga() }
                    .firstOrNull()
                    ?.toTrack()
            }
        }
    }

    suspend fun findLibAnime(track: AnimeTrack, userid: Int): AnimeTrack? {
        return withIOContext {
            val query = """
            |query (${'$'}id: Int!, ${'$'}anime_id: Int!) {
                |Page {
                    |mediaList(userId: ${'$'}id, type: ANIME, mediaId: ${'$'}anime_id) {
                        |id
                        |status
                        |scoreRaw: score(format: POINT_100)
                        |progress
                        |private
                        |startedAt {
                            |year
                            |month
                            |day
                        |}
                        |completedAt {
                            |year
                            |month
                            |day
                        |}
                        |media {
                            |id
                            |title {
                                |userPreferred
                            |}
                            |coverImage {
                                |large
                            |}
                            |format
                            |status
                            |episodes
                            |description
                            |startDate {
                                |year
                                |month
                                |day
                            |}
                            |studios {
                                |edges {
                                    |isMain
                                    |node {
                                        |name
                                    |}
                                |}
                            |}
                        |}
                    |}
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("id", userid)
                    put("anime_id", track.remote_id)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALUserListEntryQueryResult>()
                    .data.page.mediaList
                    .map { it.toALUserAnime() }
                    .firstOrNull()
                    ?.toTrack()
            }
        }
    }

    suspend fun getLibManga(track: MangaTrack, userId: Int): MangaTrack {
        return findLibManga(track, userId) ?: throw Exception("Could not find manga")
    }

    suspend fun getLibAnime(track: AnimeTrack, userId: Int): AnimeTrack {
        return findLibAnime(track, userId) ?: throw Exception("Could not find anime")
    }

    fun createOAuth(token: String): ALOAuth {
        return ALOAuth(token, "Bearer", System.currentTimeMillis() + 31536000000, 31536000000)
    }

    suspend fun getCurrentUser(): Pair<Int, String> {
        return withIOContext {
            val query = """
            |query User {
                |Viewer {
                    |id
                    |mediaListOptions {
                        |scoreFormat
                    |}
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALCurrentUserResult>()
                    .let {
                        val viewer = it.data.viewer
                        Pair(viewer.id, viewer.mediaListOptions.scoreFormat)
                    }
            }
        }
    }

    private fun createDate(dateValue: Long): JsonObject {
        if (dateValue == 0L) {
            return buildJsonObject {
                put("year", JsonNull)
                put("month", JsonNull)
                put("day", JsonNull)
            }
        }

        val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateValue), ZoneId.systemDefault())
        return buildJsonObject {
            put("year", dateTime.year)
            put("month", dateTime.monthValue)
            put("day", dateTime.dayOfMonth)
        }
    }

    companion object {
        private const val CLIENT_ID = "5338"
        private const val API_URL = "https://graphql.anilist.co/"
        private const val BASE_URL = "https://anilist.co/api/v2/"
        private const val BASE_MANGA_URL = "https://anilist.co/manga/"
        private const val BASE_ANIME_URL = "https://anilist.co/anime/"

        fun mangaUrl(mediaId: Long): String {
            return BASE_MANGA_URL + mediaId
        }

        fun animeUrl(mediaId: Long): String {
            return BASE_ANIME_URL + mediaId
        }

        fun authUrl(): Uri = "${BASE_URL}oauth/authorize".toUri().buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "token")
            .build()
    }
}
