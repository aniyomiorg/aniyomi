package eu.kanade.tachiyomi.ui.player.utils

import eu.kanade.tachiyomi.animesource.model.ChapterType
import eu.kanade.tachiyomi.animesource.model.TimeStamp
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.injectLazy

class AniSkipApi {
    private val client = OkHttpClient()
    private val json: Json by injectLazy()

    // credits: https://github.com/saikou-app/saikou/blob/main/app/src/main/java/ani/saikou/others/AniSkip.kt
    fun getResult(malId: Int, episodeNumber: Int, episodeLength: Long): List<TimeStamp>? {
        val url =
            "https://api.aniskip.com/v2/skip-times/$malId/$episodeNumber?types[]=ed" +
                "&types[]=mixed-ed&types[]=mixed-op&types[]=op&types[]=recap&episodeLength=$episodeLength"
        return try {
            val a = client.newCall(GET(url)).execute().body.string()
            val res = json.decodeFromString<AniSkipResponse>(a)
            if (res.found) {
                res.results?.map {
                    TimeStamp(
                        start = it.interval.startTime,
                        end = it.interval.endTime,
                        name = it.skipType.getString(),
                        type = it.skipType.toChapterType(),
                    )
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getMalIdFromAL(id: Long): Long {
        val query = """
                query{
                Media(id:$id){idMal}
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
        } catch (_: Exception) {
            return 0
        }
        return response.body.string().substringAfter("idMal\":").substringBefore("}")
            .toLongOrNull() ?: 0
    }
}

@Serializable
data class AniSkipResponse(
    val found: Boolean,
    val results: List<Stamp>?,
)

@Serializable
data class Stamp(
    val interval: AniSkipInterval,
    val skipType: SkipType,
)

@Serializable
enum class SkipType {
    @SerialName("op")
    OP,

    @SerialName("ed")
    ED,

    @SerialName("recap")
    RECAP,

    @SerialName("mixed-op")
    MIXED_OP, ;

    fun getString(): String {
        return when (this) {
            OP -> "Opening"
            ED -> "Ending"
            RECAP -> "Recap"
            MIXED_OP -> "Mixed-op"
        }
    }

    fun toChapterType(): ChapterType {
        return when (this) {
            OP -> ChapterType.Opening
            ED -> ChapterType.Ending
            RECAP -> ChapterType.Recap
            MIXED_OP -> ChapterType.MixedOp
        }
    }
}

@Serializable
data class AniSkipInterval(
    val startTime: Double,
    val endTime: Double,
)
