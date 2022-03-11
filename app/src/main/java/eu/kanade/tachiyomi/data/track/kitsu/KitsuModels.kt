package eu.kanade.tachiyomi.data.track.kitsu

import androidx.annotation.CallSuper
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KitsuSearchAnime(obj: JsonObject) {
    val id = obj["id"]!!.jsonPrimitive.int
    private val canonicalTitle = obj["canonicalTitle"]!!.jsonPrimitive.content
    private val episodeCount = obj["episodeCount"]?.jsonPrimitive?.intOrNull
    val subType = obj["subtype"]?.jsonPrimitive?.contentOrNull
    val original = try {
        obj["posterImage"]?.jsonObject?.get("original")?.jsonPrimitive?.content
    } catch (e: IllegalArgumentException) {
        // posterImage is sometimes a jsonNull object instead
        null
    }
    private val synopsis = obj["synopsis"]?.jsonPrimitive?.contentOrNull
    private var startDate = obj["startDate"]?.jsonPrimitive?.contentOrNull?.let {
        val outputDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        outputDf.format(Date(it.toLong() * 1000))
    }
    private val endDate = obj["endDate"]?.jsonPrimitive?.contentOrNull

    @CallSuper
    fun toTrack() = AnimeTrackSearch.create(TrackManager.KITSU).apply {
        media_id = this@KitsuSearchAnime.id
        title = canonicalTitle
        total_episodes = episodeCount ?: 0
        cover_url = original ?: ""
        summary = synopsis ?: ""
        tracking_url = KitsuApi.animeUrl(media_id)
        publishing_status = if (endDate == null) {
            "Publishing"
        } else {
            "Finished"
        }
        publishing_type = subType ?: ""
        start_date = startDate ?: ""
    }
}

class KitsuLibAnime(obj: JsonObject, anime: JsonObject) {
    val id = anime["id"]!!.jsonPrimitive.int
    private val canonicalTitle = anime["attributes"]!!.jsonObject["canonicalTitle"]!!.jsonPrimitive.content
    private val episodeCount = anime["attributes"]!!.jsonObject["episodeCount"]?.jsonPrimitive?.intOrNull
    val type = anime["attributes"]!!.jsonObject["subtype"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val original = anime["attributes"]!!.jsonObject["posterImage"]!!.jsonObject["original"]!!.jsonPrimitive.content
    private val synopsis = anime["attributes"]!!.jsonObject["synopsis"]!!.jsonPrimitive.content
    private val startDate = anime["attributes"]!!.jsonObject["startDate"]?.jsonPrimitive?.contentOrNull.orEmpty()
    private val startedAt = obj["attributes"]!!.jsonObject["startedAt"]?.jsonPrimitive?.contentOrNull
    private val finishedAt = obj["attributes"]!!.jsonObject["finishedAt"]?.jsonPrimitive?.contentOrNull
    private val libraryId = obj["id"]!!.jsonPrimitive.int
    val status = obj["attributes"]!!.jsonObject["status"]!!.jsonPrimitive.content
    private val ratingTwenty = obj["attributes"]!!.jsonObject["ratingTwenty"]?.jsonPrimitive?.contentOrNull
    val progress = obj["attributes"]!!.jsonObject["progress"]!!.jsonPrimitive.int

    fun toTrack() = AnimeTrackSearch.create(TrackManager.KITSU).apply {
        media_id = libraryId
        title = canonicalTitle
        total_episodes = episodeCount ?: 0
        cover_url = original
        summary = synopsis
        tracking_url = KitsuApi.animeUrl(media_id)
        publishing_status = this@KitsuLibAnime.status
        publishing_type = type
        start_date = startDate
        started_watching_date = KitsuDateHelper.parse(startedAt)
        finished_watching_date = KitsuDateHelper.parse(finishedAt)
        status = toTrackStatus()
        score = ratingTwenty?.let { it.toInt() / 2f } ?: 0f
        last_episode_seen = progress.toFloat()
    }

    private fun toTrackStatus() = when (status) {
        "current" -> Kitsu.WATCHING
        "completed" -> Kitsu.COMPLETED
        "on_hold" -> Kitsu.ON_HOLD
        "dropped" -> Kitsu.DROPPED
        "planned" -> Kitsu.PLAN_TO_WATCH
        else -> throw Exception("Unknown status")
    }
}

fun AnimeTrack.toKitsuStatus() = when (status) {
    Kitsu.WATCHING -> "current"
    Kitsu.COMPLETED -> "completed"
    Kitsu.ON_HOLD -> "on_hold"
    Kitsu.DROPPED -> "dropped"
    Kitsu.PLAN_TO_WATCH -> "planned"
    else -> throw Exception("Unknown status")
}

fun AnimeTrack.toKitsuScore(): String? {
    return if (score > 0) (score * 2).toInt().toString() else null
}
