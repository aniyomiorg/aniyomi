package eu.kanade.tachiyomi.data.track.simkl

import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import kotlinx.serialization.Serializable

@Serializable
data class OAuth(
    val access_token: String,
    val token_type: String,
    val scope: String,
)

fun AnimeTrack.toSimklStatus() = when (status) {
    Simkl.WATCHING -> "watching"
    Simkl.COMPLETED -> "completed"
    Simkl.ON_HOLD -> "hold"
    Simkl.NOT_INTERESTING -> "notinteresting"
    Simkl.PLAN_TO_WATCH -> "plantowatch"
    else -> throw NotImplementedError("Unknown status: $status")
}

fun toTrackStatus(status: String) = when (status) {
    "watching" -> Simkl.WATCHING
    "completed" -> Simkl.COMPLETED
    "hold" -> Simkl.ON_HOLD
    "dropped", "notinteresting" -> Simkl.NOT_INTERESTING
    "plantowatch" -> Simkl.PLAN_TO_WATCH
    else -> throw NotImplementedError("Unknown status: $status")
}
