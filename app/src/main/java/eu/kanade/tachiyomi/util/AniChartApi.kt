package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.simkl.Simkl
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.ui.entries.anime.track.AnimeTrackItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entries.anime.model.Anime
import uy.kohesive.injekt.injectLazy
import java.time.OffsetDateTime
import java.util.Calendar

class AniChartApi {
    private val client = OkHttpClient()
    private val json: Json by injectLazy()
    private val toMillis = 1000L

    internal suspend fun loadAiringTime(aniChartItems: List<AniChartItem>, manualFetch: Boolean): AnimeAiringMap {
        val anilistIdMap: TrackIdMap = mutableMapOf()
        val malIdMap: TrackIdMap = mutableMapOf()
        val simklIdMap: TrackIdMap = mutableMapOf()
        val extraIdMap: MutableMap<Long, AiringDetails> = mutableMapOf()
        aniChartItems.forEach { aniChartItem ->
            val nextEpisodeToAir = aniChartItem.anime.nextEpisodeToAir
            val nextEpisodeAiringAt = aniChartItem.anime.nextEpisodeAiringAt
            val isAnimeCompleted = aniChartItem.anime.status == SAnime.COMPLETED.toLong()
            val airingEpisodeData = AiringDetails(nextEpisodeToAir, nextEpisodeAiringAt)
            val airingDifference = nextEpisodeAiringAt.times(toMillis).minus(Calendar.getInstance().timeInMillis)

            if (!manualFetch && (isAnimeCompleted || airingDifference > 0L)) {
                extraIdMap[aniChartItem.anime.id] = airingEpisodeData
                return@forEach
            }

            val matchingTrackItem = aniChartItem.trackItems.firstOrNull {
                it.track != null && (it.tracker is Anilist || it.tracker is MyAnimeList || it.tracker is Simkl)
            }

            if (matchingTrackItem == null) {
                extraIdMap[aniChartItem.anime.id] = AiringDetails()
                return@forEach
            }

            when (matchingTrackItem.tracker) {
                is Anilist -> anilistIdMap[aniChartItem.anime.id] = matchingTrackItem.track!!.remoteId
                is MyAnimeList -> malIdMap[aniChartItem.anime.id] = matchingTrackItem.track!!.remoteId
                is Simkl -> simklIdMap[aniChartItem.anime.id] = matchingTrackItem.track!!.remoteId
            }
            aniChartItem.anime.id to airingEpisodeData
        }
        val simklAiringMap = getSimklAiringEpisodeData(simklIdMap)
        val malAiringMap = getAnilistAiringEpisodeData(true, (malIdMap + simklAiringMap.second).toMutableMap(), 1)
        val anilistAiringMap = getAnilistAiringEpisodeData(false, anilistIdMap, 1)
        return extraIdMap + simklAiringMap.first + malAiringMap + anilistAiringMap
    }

    /**
     * Each IdMap represents a Map of <animeId, track's remoteId>
     * Return type is [Pair<Map<animeId, Pair<episodeNumberToAir, episodeAiringAt>>, Map<animeId, malId>]
     */
    private suspend fun getSimklAiringEpisodeData(simklIdMap: TrackIdMap): Pair<AnimeAiringMap, TrackIdMap> {
        val malIdMap: TrackIdMap = mutableMapOf()
        val simklAiringMap = mutableMapOf<Long, AiringDetails>()
        var episodeNumber = 1
        var airingAt = 0L
        return withIOContext {
            for (calendarType in listOf("anime", "tv", "movie_release")) {
                val response = try {
                    client.newCall(GET("https://data.simkl.in/calendar/$calendarType.json")).execute()
                } catch (e: Exception) {
                    return@withIOContext Pair(simklIdMap.map { (id, _) -> id to AiringDetails() }.toMap(), malIdMap)
                }
                val body = response.body.string()
                val simklData = removeAiredSimkl(json.decodeFromString<List<SimklResponse>>(body))

                simklIdMap.forEach { (animeId, simklId) ->
                    val data = simklData.find { it.ids.simkl == simklId }

                    val malId = data?.ids?.mal?.toLongOrNull() ?: 0L
                    if (malId != 0L) {
                        malIdMap[animeId] = malId
                        simklIdMap.remove(animeId)
                        return@forEach
                    }

                    episodeNumber = data?.episode?.episode ?: episodeNumber

                    airingAt = if (data?.date?.isNotBlank() == true) toUnixTimestamp(data.date) else airingAt

                    simklAiringMap[animeId] = if (airingAt != 0L) {
                        AiringDetails(episodeNumber, airingAt)
                    } else {
                        AiringDetails()
                    }
                }
            }
            return@withIOContext Pair(simklAiringMap.toMap(), malIdMap)
        }
    }

    private fun removeAiredSimkl(simklData: List<SimklResponse>): List<SimklResponse> {
        val currentTimeInMillis = Calendar.getInstance().timeInMillis
        return simklData.filter {
            val date = it.date ?: ""
            val time = if (date.isNotBlank()) toUnixTimestamp(date) else 0L
            time.times(1000) > currentTimeInMillis
        }
    }

    private fun toUnixTimestamp(dateFormat: String): Long {
        val offsetDateTime = OffsetDateTime.parse(dateFormat)
        val instant = offsetDateTime.toInstant()
        return instant.epochSecond
    }

    private suspend fun getAnilistAiringEpisodeData(isMal: Boolean, aniIdMap: TrackIdMap, page: Int): AnimeAiringMap {
        var malAiringMap = mapOf<Long, AiringDetails>()
        val idIn = if (isMal) "idMal_in" else "id_in"
        val remoteId = if (isMal) "idMal" else "id"
        return withIOContext {
            val query = """
                query {
                  Page(page: $page, perPage: 50) {
                    media($idIn: ${aniIdMap.values.joinToString(",")}) {
                      $remoteId
                      nextAiringEpisode {
                        episode
                        airingAt
                      }
                    }
                  }
                }
            """.trimMargin()

            try {
                val response = client.newCall(
                    POST(
                        "https://graphql.anilist.co",
                        body = buildJsonObject { put("query", query) }.toString()
                            .toRequestBody(jsonMime),
                    )
                ).execute()

                val anilistResponse = response.body.let { json.decodeFromString<AnilistResponse>(it.string()) }
                val mediaList = anilistResponse.data.page.media
                if (mediaList.isNotEmpty()) {
                    malAiringMap = mediaList.associate { media ->
                        val id = if (isMal) media.idMal else media.id
                        val nextAiring = media.nextAiringEpisode
                        val epNum = nextAiring?.episode ?: 1
                        val airingAt = nextAiring?.airingAt ?: 0L

                        aniIdMap.filterValues { it == id }.keys.first() to AiringDetails(epNum, airingAt)
                    } + getAnilistAiringEpisodeData(isMal, aniIdMap, page + 1)
                }
            } catch (e: Exception) {
                return@withIOContext aniIdMap.map { (id, _) -> id to AiringDetails() }.toMap()
            }
            return@withIOContext malAiringMap
        }
    }
}

private typealias TrackIdMap = MutableMap<Long, Long>
private typealias AnimeAiringMap = Map<Long, AiringDetails>
data class AniChartItem(val anime: Anime, val trackItems: List<AnimeTrackItem>)
data class AiringDetails(val episode: Int = 1, val time: Long = 0L)

@Serializable
private class AnilistResponse(
    val data: AnilistData
)

@Serializable
private class AnilistData(
    @SerialName("Page") val page: AnilistPage
)

@Serializable
private class AnilistPage(
    val media: List<AnilistMedia>
)

@Serializable
private class AnilistMedia(
    val id: Long?,
    val idMal: Long?,
    val nextAiringEpisode: AnilistNextAiringEpisode?
)

@Serializable
private class AnilistNextAiringEpisode(
    val episode: Int?,
    val airingAt: Long?
)

@Serializable
private class SimklResponse(
    val ids: SimklIds,
    val episode: SimklEpisode? = null,
    val date: String?
)

@Serializable
private class SimklIds(
    @SerialName("simkl_id") val simkl: Long,
    val mal: String?,
)

@Serializable
private class SimklEpisode(
    val episode: Int,
)
