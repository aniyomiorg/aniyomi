package eu.kanade.tachiyomi.data.track.jellyfin

import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.track.jellyfin.dto.JFItem
import eu.kanade.tachiyomi.data.track.jellyfin.dto.JFItemList
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class JellyfinApi(
    private val trackId: Long,
    private val client: OkHttpClient,
) {
    private val json: Json by injectLazy()

    suspend fun getTrackSearch(url: String): AnimeTrackSearch =
        withIOContext {
            try {
                val httpUrl = url.toHttpUrl()
                val fragment = httpUrl.fragment!!

                val track = with(json) {
                    client.newCall(GET(url))
                        .awaitSuccess()
                        .parseAs<JFItem>()
                        .toTrack()
                }.apply { tracking_url = url }

                when {
                    fragment.startsWith("seriesId") -> {
                        getTrackFromSeries(track, httpUrl)
                    }
                    else -> track
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Could not get item: $url" }
                throw e
            }
        }

    private fun JFItem.toTrack(): AnimeTrackSearch = AnimeTrackSearch.create(
        trackId,
    ).also {
        it.title = name
        it.total_episodes = 1
        if (userData.played) {
            it.last_episode_seen = 1.0
            it.status = Jellyfin.COMPLETED
        } else {
            it.last_episode_seen = 0.0
            it.status = Jellyfin.UNSEEN
        }
    }

    private fun getEpisodesUrl(url: HttpUrl): HttpUrl {
        val fragment = url.fragment!!
        val usersIndex = url.pathSegments.indexOf("Users")
        val baseUrl = if(usersIndex != -1) url.newBuilder().apply {
            fragment(null)
            encodedQuery(null)
            encodedPath("/" + url.pathSegments.subList(0, usersIndex).joinToString("/"))
        } else url.newBuilder().apply {
            encodedPath("/")
            fragment(null)
            encodedQuery(null)
        }
        val userId = if(usersIndex != -1) url.pathSegments[usersIndex + 1] else url.pathSegments[1]

        return baseUrl.apply {
            addPathSegment("Shows")
            addPathSegment(fragment.split(",").last())
            addPathSegment("Episodes")
            addQueryParameter("seasonId", url.pathSegments.last())
            addQueryParameter("userId", userId)
            addQueryParameter("Fields", "Overview,MediaSources")
        }.build()
    }

    private suspend fun getTrackFromSeries(track: AnimeTrackSearch, url: HttpUrl): AnimeTrackSearch {
        val episodesUrl = getEpisodesUrl(url)

        val episodes = with(json) {
            client.newCall(GET(episodesUrl))
                .awaitSuccess()
                .parseAs<JFItemList>()
        }.items

        val totalEpisodes = episodes.last().indexNumber!!
        val firstUnwatched = episodes.indexOfFirst { !it.userData.played }

        if (firstUnwatched == 0) {
            return track.apply {
                this.total_episodes = totalEpisodes
                this.last_episode_seen = 0.0
                this.status = Jellyfin.UNSEEN
            }
        }

        if (firstUnwatched == -1) {
            return track.apply {
                this.total_episodes = totalEpisodes
                this.last_episode_seen = totalEpisodes.toDouble()
                this.status = Jellyfin.COMPLETED
            }
        }

        val lastContinuousSeen = episodes[firstUnwatched - 1].indexNumber!!

        return track.apply {
            this.total_episodes = totalEpisodes
            this.last_episode_seen = lastContinuousSeen.toDouble()
            this.status = Jellyfin.WATCHING
        }
    }

    suspend fun updateProgress(track: AnimeTrack): AnimeTrack {
        val httpUrl = track.tracking_url.toHttpUrl()
        val fragment = httpUrl.fragment!!

        val itemId = if (fragment.startsWith("movie")) {
            httpUrl.pathSegments.last()
        } else {
            val episodesUrl = getEpisodesUrl(httpUrl)
            val episodes = with(json) {
                client.newCall(GET(episodesUrl))
                    .awaitSuccess()
                    .parseAs<JFItemList>()
            }.items

            episodes.firstOrNull {
                it.indexNumber!!.equalsTo(track.last_episode_seen)
            }?.id
        }

        if (itemId != null) {
            val usersIndex = httpUrl.pathSegments.indexOf("Users")
            if(usersIndex != -1) {
                val time = DATE_FORMATTER.format(Date())
                val postUrl = httpUrl.newBuilder().apply {
                    fragment(null)
                    encodedPath("/" + httpUrl.pathSegments.subList(0, usersIndex + 2).joinToString("/"))
                    addPathSegment("PlayedItems")
                    addPathSegment(itemId)
                    addQueryParameter("DatePlayed", time)
                }.build().toString()

                client.newCall(
                    POST(postUrl),
                ).awaitSuccess()
            }
        }

        return getTrackSearch(track.tracking_url)
    }

    private fun Long.equalsTo(other: Double): Boolean {
        return abs(this - other) < 0.001
    }

    companion object {
        private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    }
}
