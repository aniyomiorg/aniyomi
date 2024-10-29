package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack
import tachiyomi.domain.track.manga.model.MangaTrack as DomainMangaTrack

fun MangaTrack.toApiStatus() = when (status) {
    Anilist.READING -> "CURRENT"
    Anilist.COMPLETED -> "COMPLETED"
    Anilist.ON_HOLD -> "PAUSED"
    Anilist.DROPPED -> "DROPPED"
    Anilist.PLAN_TO_READ -> "PLANNING"
    Anilist.REREADING -> "REPEATING"
    else -> throw NotImplementedError("Unknown status: $status")
}

fun AnimeTrack.toApiStatus() = when (status) {
    Anilist.WATCHING -> "CURRENT"
    Anilist.COMPLETED -> "COMPLETED"
    Anilist.ON_HOLD -> "PAUSED"
    Anilist.DROPPED -> "DROPPED"
    Anilist.PLAN_TO_WATCH -> "PLANNING"
    Anilist.REWATCHING -> "REPEATING"
    else -> throw NotImplementedError("Unknown status: $status")
}

private val preferences: TrackPreferences by injectLazy()

private fun Double.toApiScore(): String = when (preferences.anilistScoreType().get()) {
    // 10 point
    "POINT_10" -> (this.toInt() / 10).toString()
    // 100 point
    "POINT_100" -> this.toInt().toString()
    // 5 stars
    "POINT_5" -> when {
        this == 0.0 -> "0"
        this < 30 -> "1"
        this < 50 -> "2"
        this < 70 -> "3"
        this < 90 -> "4"
        else -> "5"
    }
    // Smiley
    "POINT_3" -> when {
        this == 0.0 -> "0"
        this <= 35 -> ":("
        this <= 60 -> ":|"
        else -> ":)"
    }
    // 10 point decimal
    "POINT_10_DECIMAL" -> (this / 10).toString()
    else -> throw NotImplementedError("Unknown score type")
}

fun DomainMangaTrack.toApiScore(): String = this.score.toApiScore()
fun DomainAnimeTrack.toApiScore(): String = this.score.toApiScore()
