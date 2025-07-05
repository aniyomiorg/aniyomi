package eu.kanade.tachiyomi.data.track.myanimelist

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALAnime
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALListAnimeItem
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALListAnimeItemStatus
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALListMangaItem
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALListMangaItemStatus
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALManga
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALSearchResult
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALUser
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALUserSearchResult
import eu.kanade.tachiyomi.network.DELETE
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.PkceUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack
import tachiyomi.domain.track.manga.model.MangaTrack as DomainMangaTrack

class MyAnimeListApi(
    private val trackId: Long,
    private val client: OkHttpClient,
    interceptor: MyAnimeListInterceptor,
) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    suspend fun getAccessToken(authCode: String): MALOAuth {
        return withIOContext {
            val formBody: RequestBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("code", authCode)
                .add("code_verifier", codeVerifier)
                .add("grant_type", "authorization_code")
                .build()
            with(json) {
                client.newCall(POST("$BASE_OAUTH_URL/token", body = formBody))
                    .awaitSuccess()
                    .parseAs()
            }
        }
    }

    suspend fun getCurrentUser(): String {
        return withIOContext {
            val request = Request.Builder()
                .url("$BASE_API_URL/users/@me")
                .get()
                .build()
            with(json) {
                authClient.newCall(request)
                    .awaitSuccess()
                    .parseAs<MALUser>()
                    .name
            }
        }
    }

    suspend fun search(query: String): List<MangaTrackSearch> {
        return withIOContext {
            val url = "$BASE_API_URL/manga".toUri().buildUpon()
                // MAL API throws a 400 when the query is over 64 characters...
                .appendQueryParameter("q", query.take(64))
                .appendQueryParameter("nsfw", "true")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MALSearchResult>()
                    .data
                    .map { async { getMangaDetails(it.node.id) } }
                    .awaitAll()
                    .filter { !it.publishing_type.contains("novel") }
            }
        }
    }

    suspend fun searchAnime(query: String): List<AnimeTrackSearch> {
        return withIOContext {
            val url = "$BASE_API_URL/anime".toUri().buildUpon()
                // MAL API throws a 400 when the query is over 64 characters...
                .appendQueryParameter("q", query.take(64))
                .appendQueryParameter("q", query)
                .appendQueryParameter("nsfw", "true")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MALSearchResult>()
                    .data
                    .map { async { getAnimeDetails(it.node.id) } }
                    .awaitAll()
            }
        }
    }

    suspend fun getMangaDetails(id: Int): MangaTrackSearch {
        return withIOContext {
            val url = "$BASE_API_URL/manga".toUri().buildUpon()
                .appendPath(id.toString())
                .appendQueryParameter(
                    "fields",
                    "id,title,synopsis,num_chapters,mean,main_picture,status,media_type,start_date",
                )
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MALManga>()
                    .let {
                        MangaTrackSearch.create(trackId).apply {
                            remote_id = it.id
                            title = it.title
                            summary = it.synopsis
                            total_chapters = it.numChapters
                            score = it.mean
                            cover_url = it.covers?.large.orEmpty()
                            tracking_url = "https://myanimelist.net/manga/$remote_id"
                            publishing_status = it.status.replace("_", " ")
                            publishing_type = it.mediaType.replace("_", " ")
                            start_date = it.startDate ?: ""
                        }
                    }
            }
        }
    }

    suspend fun getAnimeDetails(id: Int): AnimeTrackSearch {
        return withIOContext {
            val url = "$BASE_API_URL/anime".toUri().buildUpon()
                .appendPath(id.toString())
                .appendQueryParameter(
                    "fields",
                    "id,title,synopsis,num_episodes,mean,main_picture,status,media_type,start_date",
                )
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MALAnime>()
                    .let {
                        AnimeTrackSearch.create(trackId).apply {
                            remote_id = it.id
                            title = it.title
                            summary = it.synopsis
                            total_episodes = it.numEpisodes
                            score = it.mean
                            cover_url = it.covers?.large.orEmpty()
                            tracking_url = "https://myanimelist.net/anime/$remote_id"
                            publishing_status = it.status.replace("_", " ")
                            publishing_type = it.mediaType.replace("_", " ")
                            start_date = it.startDate ?: ""
                        }
                    }
            }
        }
    }

    suspend fun updateItem(track: MangaTrack): MangaTrack {
        return withIOContext {
            val formBodyBuilder = FormBody.Builder()
                .add("status", track.toMyAnimeListStatus() ?: "reading")
                .add("is_rereading", (track.status == MyAnimeList.REREADING).toString())
                .add("score", track.score.toString())
                .add("num_chapters_read", track.last_chapter_read.toInt().toString())
            convertToIsoDate(track.started_reading_date)?.let {
                formBodyBuilder.add("start_date", it)
            }
            convertToIsoDate(track.finished_reading_date)?.let {
                formBodyBuilder.add("finish_date", it)
            }

            val request = Request.Builder()
                .url(mangaUrl(track.remote_id).toString())
                .put(formBodyBuilder.build())
                .build()
            with(json) {
                authClient.newCall(request)
                    .awaitSuccess()
                    .parseAs<MALListMangaItemStatus>()
                    .let { parseMangaItem(it, track) }
            }
        }
    }

    suspend fun updateItem(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val formBodyBuilder = FormBody.Builder()
                .add("status", track.toMyAnimeListStatus() ?: "watching")
                .add("is_rewatching", (track.status == MyAnimeList.REWATCHING).toString())
                .add("score", track.score.toString())
                .add("num_watched_episodes", track.last_episode_seen.toInt().toString())
            convertToIsoDate(track.started_watching_date)?.let {
                formBodyBuilder.add("start_date", it)
            }
            convertToIsoDate(track.finished_watching_date)?.let {
                formBodyBuilder.add("finish_date", it)
            }

            val request = Request.Builder()
                .url(animeUrl(track.remote_id).toString())
                .put(formBodyBuilder.build())
                .build()
            with(json) {
                authClient.newCall(request)
                    .awaitSuccess()
                    .parseAs<MALListAnimeItemStatus>()
                    .let { parseAnimeItem(it, track) }
            }
        }
    }

    suspend fun deleteMangaItem(track: DomainMangaTrack) {
        withIOContext {
            authClient
                .newCall(DELETE(mangaUrl(track.remoteId).toString()))
                .awaitSuccess()
        }
    }

    suspend fun deleteAnimeItem(track: DomainAnimeTrack) {
        withIOContext {
            authClient
                .newCall(DELETE(animeUrl(track.remoteId).toString()))
                .awaitSuccess()
        }
    }

    suspend fun findListItem(track: MangaTrack): MangaTrack? {
        return withIOContext {
            val uri = "$BASE_API_URL/manga".toUri().buildUpon()
                .appendPath(track.remote_id.toString())
                .appendQueryParameter("fields", "num_chapters,my_list_status{start_date,finish_date}")
                .build()
            with(json) {
                authClient.newCall(GET(uri.toString()))
                    .awaitSuccess()
                    .parseAs<MALListMangaItem>()
                    .let { item ->
                        track.total_chapters = item.numChapters
                        item.myListStatus?.let { parseMangaItem(it, track) }
                    }
            }
        }
    }

    suspend fun findListItem(track: AnimeTrack): AnimeTrack? {
        return withIOContext {
            val uri = "$BASE_API_URL/anime".toUri().buildUpon()
                .appendPath(track.remote_id.toString())
                .appendQueryParameter("fields", "num_episodes,my_list_status{start_date,finish_date}")
                .build()
            with(json) {
                authClient.newCall(GET(uri.toString()))
                    .awaitSuccess()
                    .parseAs<MALListAnimeItem>()
                    .let { item ->
                        track.total_episodes = item.numEpisodes
                        item.myListStatus?.let { parseAnimeItem(it, track) }
                    }
            }
        }
    }

    suspend fun findListItems(query: String, offset: Int = 0): List<MangaTrackSearch> {
        return withIOContext {
            val myListSearchResult = getListPage(offset)

            val matches = myListSearchResult.data
                .filter { it.node.title.contains(query, ignoreCase = true) }
                .map { async { getMangaDetails(it.node.id) } }
                .awaitAll()

            // Check next page if there's more
            if (!myListSearchResult.paging.next.isNullOrBlank()) {
                matches + findListItems(query, offset + LIST_PAGINATION_AMOUNT)
            } else {
                matches
            }
        }
    }

    suspend fun findListItemsAnime(query: String, offset: Int = 0): List<AnimeTrackSearch> {
        return withIOContext {
            val myListSearchResult = getListPage(offset)

            val matches = myListSearchResult.data
                .filter { it.node.title.contains(query, ignoreCase = true) }
                .map { async { getAnimeDetails(it.node.id) } }
                .awaitAll()

            // Check next page if there's more
            if (!myListSearchResult.paging.next.isNullOrBlank()) {
                matches + findListItemsAnime(query, offset + LIST_PAGINATION_AMOUNT)
            } else {
                matches
            }
        }
    }

    private suspend fun getListPage(offset: Int): MALUserSearchResult {
        return withIOContext {
            val urlBuilder = "$BASE_API_URL/users/@me/mangalist".toUri().buildUpon()
                .appendQueryParameter("fields", "list_status{start_date,finish_date}")
                .appendQueryParameter("limit", LIST_PAGINATION_AMOUNT.toString())
            if (offset > 0) {
                urlBuilder.appendQueryParameter("offset", offset.toString())
            }

            val request = Request.Builder()
                .url(urlBuilder.build().toString())
                .get()
                .build()
            with(json) {
                authClient.newCall(request)
                    .awaitSuccess()
                    .parseAs()
            }
        }
    }

    private fun parseMangaItem(listStatus: MALListMangaItemStatus, track: MangaTrack): MangaTrack {
        return track.apply {
            val isRereading = listStatus.isRereading
            status = if (isRereading) MyAnimeList.REREADING else getStatus(listStatus.status)
            last_chapter_read = listStatus.numChaptersRead
            score = listStatus.score.toDouble()
            listStatus.startDate?.let { started_reading_date = parseDate(it) }
            listStatus.finishDate?.let { finished_reading_date = parseDate(it) }
        }
    }

    private fun parseAnimeItem(listStatus: MALListAnimeItemStatus, track: AnimeTrack): AnimeTrack {
        return track.apply {
            val isRewatching = listStatus.isRewatching
            status = if (isRewatching) MyAnimeList.REWATCHING else getStatus(listStatus.status)
            last_episode_seen = listStatus.numEpisodesWatched
            score = listStatus.score.toDouble()
            listStatus.startDate?.let { started_watching_date = parseDate(it) }
            listStatus.finishDate?.let { finished_watching_date = parseDate(it) }
        }
    }

    private fun parseDate(isoDate: String): Long {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate)?.time ?: 0L
    }

    private fun convertToIsoDate(epochTime: Long): String? {
        if (epochTime == 0L) {
            return ""
        }
        return try {
            val outputDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            outputDf.format(epochTime)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val CLIENT_ID = "686b980ff4240fccce7f6a654cea07ce"

        private const val BASE_OAUTH_URL = "https://myanimelist.net/v1/oauth2"
        private const val BASE_API_URL = "https://api.myanimelist.net/v2"

        private const val LIST_PAGINATION_AMOUNT = 250

        private var codeVerifier: String = ""

        fun authUrl(): Uri = "$BASE_OAUTH_URL/authorize".toUri().buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("code_challenge", getPkceChallengeCode())
            .appendQueryParameter("response_type", "code")
            .build()

        fun mangaUrl(id: Long): Uri = "$BASE_API_URL/manga".toUri().buildUpon()
            .appendPath(id.toString())
            .appendPath("my_list_status")
            .build()

        fun animeUrl(id: Long): Uri = "$BASE_API_URL/anime".toUri().buildUpon()
            .appendPath(id.toString())
            .appendPath("my_list_status")
            .build()

        fun refreshTokenRequest(oauth: MALOAuth): Request {
            val formBody: RequestBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("refresh_token", oauth.refreshToken)
                .add("grant_type", "refresh_token")
                .build()

            // Add the Authorization header manually as this particular
            // request is called by the interceptor itself so it doesn't reach
            // the part where the token is added automatically.
            val headers = Headers.Builder()
                .add("Authorization", "Bearer ${oauth.accessToken}")
                .build()

            return POST("$BASE_OAUTH_URL/token", body = formBody, headers = headers)
        }

        private fun getPkceChallengeCode(): String {
            codeVerifier = PkceUtil.generateCodeVerifier()
            return codeVerifier
        }
    }
}
