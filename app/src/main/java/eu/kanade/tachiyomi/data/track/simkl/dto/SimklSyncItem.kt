package eu.kanade.tachiyomi.data.track.simkl.dto

import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.simkl.toTrackStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimklSyncResult(
    val anime: List<SimklSyncItem>?,
    val tv: List<SimklSyncItem>?,
    val movies: List<SimklSyncItem>?,
) {
    fun getFromType(type: String): List<SimklSyncItem>? {
        return when (type) {
            "anime" -> anime
            "tv" -> tv
            "movies" -> movies
            else -> throw Exception("Unknown type: $type")
        }
    }
}

@Serializable
data class SimklSyncItem(
    val show: SimklSyncResultItem?,
    val movie: SimklSyncResultItem?,
    @SerialName("total_episodes_count")
    val totalEpisodesCount: Long?,
    @SerialName("watched_episodes_count")
    val watchedEpisodesCount: Double?,
    @SerialName("user_rating")
    val userRating: Int?,
) {
    fun toAnimeTrack(typeName: String, type: String, statusString: String): AnimeTrack {
        val resultData = getFromType(typeName)

        return AnimeTrack.create(TrackerManager.SIMKL).apply {
            title = resultData.title
            remote_id = resultData.ids.simkl
            if (typeName != "movie") {
                total_episodes = totalEpisodesCount!!
                last_episode_seen = watchedEpisodesCount!!
            } else {
                total_episodes = 1
                last_episode_seen = if (statusString == "completed") 1.0 else 0.0
            }
            score = userRating?.toDouble() ?: 0.0
            status = toTrackStatus(statusString)
            tracking_url = "/$type/${resultData.ids.simkl}"
        }
    }

    fun getFromType(typeName: String): SimklSyncResultItem {
        return when (typeName) {
            "show" -> show!!
            "movie" -> movie!!
            else -> throw Exception("Unknown type: $typeName")
        }
    }
}

@Serializable
data class SimklSyncResultItem(
    val title: String,
    val ids: SimklSyncResultIds,
)

@Serializable
data class SimklSyncResultIds(
    val simkl: Long,
)
