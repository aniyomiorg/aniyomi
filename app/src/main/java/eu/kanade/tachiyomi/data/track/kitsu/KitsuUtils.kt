package eu.kanade.tachiyomi.data.track.kitsu

import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack

fun MangaTrack.toApiStatus() = when (status) {
    Kitsu.READING -> "current"
    Kitsu.COMPLETED -> "completed"
    Kitsu.ON_HOLD -> "on_hold"
    Kitsu.DROPPED -> "dropped"
    Kitsu.PLAN_TO_READ -> "planned"
    else -> throw Exception("Unknown status")
}

fun AnimeTrack.toApiStatus() = when (status) {
    Kitsu.WATCHING -> "current"
    Kitsu.COMPLETED -> "completed"
    Kitsu.ON_HOLD -> "on_hold"
    Kitsu.DROPPED -> "dropped"
    Kitsu.PLAN_TO_WATCH -> "planned"
    else -> throw Exception("Unknown status")
}

fun MangaTrack.toApiScore(): String? {
    return if (score > 0) (score * 2).toInt().toString() else null
}

fun AnimeTrack.toApiScore(): String? {
    return if (score > 0) (score * 2).toInt().toString() else null
}
