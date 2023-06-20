package eu.kanade.tachiyomi.util

import eu.kanade.domain.entries.anime.interactor.SetAnimeViewerFlags
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.ui.entries.anime.track.AnimeTrackItem
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.domain.items.episode.interactor.GetEpisodeByAnimeId
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AniChartApi {
    private val client = OkHttpClient()
    private val getEpisodesByAnimeId: GetEpisodeByAnimeId = Injekt.get()
    private val setAnimeViewerFlags: SetAnimeViewerFlags = Injekt.get()

    internal suspend fun loadAiringTime(animeId: Long, trackItems: List<AnimeTrackItem>): Long {
        return withIOContext {
            val episodes = getEpisodesByAnimeId.await(animeId)
            var alId: Long? = 0L
            var airingTime = 0L
            trackItems.forEach {
                if (it.track != null) {
                    alId = when (it.service) {
                        is Anilist -> it.track.media_id
                        is MyAnimeList -> getAlIdFromMal(it.track.media_id)
                        else -> null
                    }
                }
            }
            if (alId != null && episodes.isNotEmpty()) {
                val latestEpisode = episodes.maxByOrNull { it.episodeNumber }!!
                val episodeToAir = latestEpisode.episodeNumber.toInt() + 1
                airingTime = getAiringAt(alId!!, episodeToAir)
                setAnimeViewerFlags.awaitSetNextEpisodeAiringAt(animeId, airingTime)
            }
            return@withIOContext airingTime
        }
    }

    private suspend fun getAlIdFromMal(idMal: Long): Long {
        return withIOContext {
            val query = """
                query {
                    Media(idMal:$idMal,type: ANIME) {
                        id
                    }
                }
            """.trimMargin()

            val response = try {
                client.newCall(
                    POST(
                        "https://graphql.anilist.co",
                        body = buildJsonObject { put("query", query) }.toString()
                            .toRequestBody(jsonMime),
                    ),
                ).execute()
            } catch (e: Exception) {
                return@withIOContext 0L
            }
            return@withIOContext response.body.string().substringAfter("id\":")
                .substringBefore("}")
                .toLongOrNull() ?: 0L
        }
    }

    private suspend fun getAiringAt(id: Long, episodeNumber: Int): Long {
        return withIOContext {
            val query = """
                query {
                AiringSchedule(mediaId:$id, episode: $episodeNumber) {
                    airingAt
                    }
                }
            """.trimMargin()
            val response = try {
                client.newCall(
                    POST(
                        "https://graphql.anilist.co",
                        body = buildJsonObject { put("query", query) }.toString()
                            .toRequestBody(jsonMime),
                    ),
                ).execute()
            } catch (e: Exception) {
                return@withIOContext 0L
            }
            return@withIOContext response.body.string().substringAfter("airingAt\":")
                .substringBefore("}")
                .toLongOrNull() ?: 0L
        }
    }
}
