package eu.kanade.tachiyomi.util
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.simkl.Simkl
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.ui.entries.anime.track.AnimeTrackItem
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.domain.entries.anime.model.Anime
import java.time.OffsetDateTime
import java.util.Calendar

class AniChartApi {
    private val client = OkHttpClient()

    internal suspend fun loadAiringTime(anime: Anime, trackItems: List<AnimeTrackItem>, manualFetch: Boolean): Pair<Int, Long> {
        var airingEpisodeData = Pair(anime.nextEpisodeToAir, anime.nextEpisodeAiringAt)
        if (anime.status == SAnime.COMPLETED.toLong() && !manualFetch) return airingEpisodeData

        return withIOContext {
            val matchingTrackItem = trackItems.firstOrNull {
                (it.service is Anilist && it.track != null) ||
                    (it.service is MyAnimeList && it.track != null) ||
                    (it.service is Simkl && it.track != null)
            } ?: return@withIOContext Pair(1, 0L)

            matchingTrackItem.let { item ->
                item.track!!.let {
                    airingEpisodeData = when (item.service) {
                        is Anilist -> getAnilistAiringEpisodeData(it.remoteId)
                        is MyAnimeList -> getAnilistAiringEpisodeData(getAlIdFromMal(it.remoteId))
                        is Simkl -> getSimklAiringEpisodeData(it.remoteId)
                        else -> Pair(1, 0L)
                    }
                }
            }
            return@withIOContext airingEpisodeData
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

    private suspend fun getAnilistAiringEpisodeData(id: Long): Pair<Int, Long> {
        return withIOContext {
            val query = """
                query {
                    Media(id:$id) {
                        nextAiringEpisode {
                            episode
                            airingAt
                        }
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
                return@withIOContext Pair(1, 0L)
            }
            val data = response.body.string()
            val episodeNumber = data.substringAfter("episode\":").substringBefore(",").toIntOrNull() ?: 1
            val airingAt = data.substringAfter("airingAt\":").substringBefore("}").toLongOrNull() ?: 0L

            return@withIOContext Pair(episodeNumber, airingAt)
        }
    }

    private suspend fun getSimklAiringEpisodeData(id: Long): Pair<Int, Long> {
        var episodeNumber = 1
        var airingAt = 0L
        return withIOContext {
            val calendarTypes = listOf("anime", "tv", "movie_release")
            calendarTypes.forEach {
                val response = try {
                    client.newCall(GET("https://data.simkl.in/calendar/$it.json")).execute()
                } catch (e: Exception) {
                    return@withIOContext Pair(1, 0L)
                }

                val body = response.body.string()

                val data = removeAiredSimkl(body)

                val malId = data.substringAfter("\"simkl_id\":$id,", "").substringAfter("\"mal\":\"").substringBefore("\"").toLongOrNull() ?: 0L
                if (malId != 0L) return@withIOContext getAnilistAiringEpisodeData(getAlIdFromMal(malId))

                val epNum = data.substringAfter("\"simkl_id\":$id,", "").substringBefore("\"}}").substringAfterLast("\"episode\":")
                episodeNumber = epNum.substringBefore(",").toIntOrNull() ?: episodeNumber

                val date = data.substringBefore("\"simkl_id\":$id,", "").substringAfterLast("\"date\":\"").substringBefore("\"")
                airingAt = if (date.isNotBlank()) toUnixTimestamp(date) else airingAt

                if (airingAt != 0L) return@withIOContext Pair(episodeNumber, airingAt)
            }
            return@withIOContext Pair(episodeNumber, airingAt)
        }
    }

    private fun removeAiredSimkl(body: String): String {
        val currentTimeInMillis = Calendar.getInstance().timeInMillis
        val index = body.split("\"date\":\"").drop(1).indexOfFirst {
            val date = it.substringBefore("\"")
            val time = if (date.isNotBlank()) toUnixTimestamp(date) else 0L
            time.times(1000) > currentTimeInMillis
        }
        return if (index >= 0) body.substring(index) else ""
    }

    private fun toUnixTimestamp(dateFormat: String): Long {
        val offsetDateTime = OffsetDateTime.parse(dateFormat)
        val instant = offsetDateTime.toInstant()
        return instant.epochSecond
    }
}
