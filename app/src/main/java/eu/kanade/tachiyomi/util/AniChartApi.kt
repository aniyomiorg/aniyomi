package eu.kanade.tachiyomi.util
import eu.kanade.domain.entries.anime.interactor.SetAnimeViewerFlags
import eu.kanade.tachiyomi.animesource.model.SAnime
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
import tachiyomi.domain.entries.anime.model.Anime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AniChartApi {
    private val client = OkHttpClient()
    private val setAnimeViewerFlags: SetAnimeViewerFlags = Injekt.get()

    internal suspend fun loadAiringTime(anime: Anime, trackItems: List<AnimeTrackItem>, manualFetch: Boolean): Pair<Int, Long> {
        if (anime.status == SAnime.COMPLETED.toLong() && !manualFetch) return Pair(anime.nextEpisodeToAir, anime.nextEpisodeAiringAt)
        return withIOContext {
            var alId = 0L
            var airingTime = Pair(0, 0L)
            trackItems.forEach {
                if (it.track != null) {
                    alId = when (it.service) {
                        is Anilist -> it.track.remoteId
                        is MyAnimeList -> getAlIdFromMal(it.track.remoteId)
                        else -> 0L
                    }
                }
            }
            if (alId != 0L) {
                airingTime = getAiringAt(alId)
                setAnimeViewerFlags.awaitSetNextEpisodeAiring(anime.id, airingTime)
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

    private suspend fun getAiringAt(id: Long): Pair<Int, Long> {
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
                return@withIOContext Pair(0, 0L)
            }
            val data = response.body.string()
            val episodeNumber = data.substringAfter("episode\":").substringBefore(",").toIntOrNull() ?: 0
            val airingAt = data.substringAfter("airingAt\":").substringBefore("}").toLongOrNull() ?: 0L

            return@withIOContext Pair(episodeNumber, airingAt)
        }
    }
}
