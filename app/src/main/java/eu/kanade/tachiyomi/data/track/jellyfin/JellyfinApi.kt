package eu.kanade.tachiyomi.data.track.jellyfin

import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
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
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Date
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
                        .parseAs<ItemDto>()
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

    private fun ItemDto.toTrack(): AnimeTrackSearch = AnimeTrackSearch.create(
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
        val apiKey = url.queryParameter("api_key")!!
        val fragment = url.fragment!!

        return url.newBuilder().apply {
            encodedPath("/")
            fragment(null)
            encodedQuery(null)

            addPathSegment("Shows")
            addPathSegment(fragment.split(",").last())
            addPathSegment("Episodes")
            addQueryParameter("api_key", apiKey)
            addQueryParameter("seasonId", url.pathSegments.last())
            addQueryParameter("userId", url.pathSegments[1])
            addQueryParameter("Fields", "Overview,MediaSources")
        }.build()
    }

    private suspend fun getTrackFromSeries(track: AnimeTrackSearch, url: HttpUrl): AnimeTrackSearch {
        val episodesUrl = getEpisodesUrl(url)

        val episodes = with(json) {
            client.newCall(GET(episodesUrl))
                .awaitSuccess()
                .parseAs<ItemsDto>()
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
                    .parseAs<ItemsDto>()
            }.items

            episodes.firstOrNull {
                it.indexNumber!!.equalsTo(track.last_episode_seen)
            }?.id
        }

        if (itemId != null) {
            val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(Date())
            val postUrl = httpUrl.newBuilder().apply {
                fragment(null)
                removePathSegment(3)
                removePathSegment(2)
                addPathSegment("PlayedItems")
                addPathSegment(itemId)
                addQueryParameter("DatePlayed", time)
            }.build().toString()

            client.newCall(
                POST(postUrl),
            ).awaitSuccess()
        }

        return getTrackSearch(track.tracking_url)
    }

    private fun Long.equalsTo(other: Double): Boolean {
        return abs(this - other) < 0.001
    }
}
