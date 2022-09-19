package eu.kanade.tachiyomi.util

import android.util.Log
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
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
    fun getResult(malId: Int, episodeNumber: Int, episodeLength: Long): List<Stamp>? {
        Log.i("bruh", "mal : $malId, num : $episodeNumber, len : $episodeLength")
        val url =
            "https://api.aniskip.com/v2/skip-times/$malId/$episodeNumber?types[]=ed&types[]=mixed-ed&types[]=mixed-op&types[]=op&types[]=recap&episodeLength=$episodeLength"
        return try {
            val a = client.newCall(GET(url)).execute().body!!.string()
            Log.i("bruh", "res: $a")
            val res = json.decodeFromString<AniSkipResponse>(a)
            if (res.found) res.results else null
        } catch (e: Exception) {
            null
        }
    }

    fun getMalIdFromAL(id: Long): Long {
        val query = """
                query{
                Media(id:$id){idMal}
                }
        """.trimMargin()
        val payload = buildJsonObject {
            put("query", query)
        }
        val response = client.newCall(
            POST(
                "https://graphql.anilist.co",
                body = payload.toString().toRequestBody(jsonMime),
            ),
        ).execute()
        // i need to change this
        val malId = response.body!!.string().substringAfter("idMal\":").substringBefore("}")
        return malId.toLong()
    }
}

@Serializable
data class AniSkipResponse(
    val found: Boolean,
    val results: List<Stamp>?,
    val message: String?,
    val statusCode: Int,
)

@Serializable
data class Stamp(
    val interval: AniSkipInterval,
    val skipType: SkipType,
    val skipId: String,
    val episodeLength: Double,
)

@Suppress("EnumEntryName")
enum class SkipType {
    op, ed, recap;

    fun getString(): String {
        return when (this) {
            op -> "Opening"
            ed -> "Ending"
            recap -> "Recap"
        }
    }
}

@Serializable
data class AniSkipInterval(
    val startTime: Double,
    val endTime: Double,
)
