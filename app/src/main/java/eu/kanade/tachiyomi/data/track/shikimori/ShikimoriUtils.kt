package eu.kanade.tachiyomi.data.track.shikimori

import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack

fun MangaTrack.toShikimoriStatus() = when (status) {
    Shikimori.READING -> "watching"
    Shikimori.COMPLETED -> "completed"
    Shikimori.ON_HOLD -> "on_hold"
    Shikimori.DROPPED -> "dropped"
    Shikimori.PLAN_TO_READ -> "planned"
    Shikimori.REREADING -> "rewatching"
    else -> throw NotImplementedError("Unknown status: $status")
}

fun AnimeTrack.toShikimoriStatus() = when (status) {
    Shikimori.READING -> "watching"
    Shikimori.COMPLETED -> "completed"
    Shikimori.ON_HOLD -> "on_hold"
    Shikimori.DROPPED -> "dropped"
    Shikimori.PLAN_TO_READ -> "planned"
    Shikimori.REREADING -> "rewatching"
    else -> throw NotImplementedError("Unknown status: $status")
}

fun toTrackStatus(status: String) = when (status) {
    "watching" -> Shikimori.READING
    "completed" -> Shikimori.COMPLETED
    "on_hold" -> Shikimori.ON_HOLD
    "dropped" -> Shikimori.DROPPED
    "planned" -> Shikimori.PLAN_TO_READ
    "rewatching" -> Shikimori.REREADING
    else -> throw NotImplementedError("Unknown status: $status")
}
