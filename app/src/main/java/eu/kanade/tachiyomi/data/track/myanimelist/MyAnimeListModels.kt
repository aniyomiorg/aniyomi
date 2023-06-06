package eu.kanade.tachiyomi.data.track.myanimelist

import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import kotlinx.serialization.Serializable

@Serializable
data class OAuth(
    val refresh_token: String,
    val access_token: String,
    val token_type: String,
    val created_at: Long = System.currentTimeMillis(),
    val expires_in: Long,
)

fun OAuth.isExpired() = System.currentTimeMillis() > created_at + (expires_in * 1000)

fun MangaTrack.toMyAnimeListStatus() = when (status) {
    MyAnimeList.READING -> "reading"
    MyAnimeList.COMPLETED -> "completed"
    MyAnimeList.ON_HOLD -> "on_hold"
    MyAnimeList.DROPPED -> "dropped"
    MyAnimeList.PLAN_TO_READ -> "plan_to_read"
    MyAnimeList.REREADING -> "reading"
    else -> null
}

fun AnimeTrack.toMyAnimeListStatus() = when (status) {
    MyAnimeList.WATCHING -> "watching"
    MyAnimeList.READING -> "watching"
    MyAnimeList.COMPLETED -> "completed"
    MyAnimeList.ON_HOLD -> "on_hold"
    MyAnimeList.DROPPED -> "dropped"
    MyAnimeList.PLAN_TO_READ -> "plan_to_watch"
    MyAnimeList.REREADING -> "watching"
    MyAnimeList.PLAN_TO_WATCH -> "plan_to_watch"
    MyAnimeList.REWATCHING -> "watching"
    else -> null
}

fun getStatus(status: String?) = when (status) {
    "reading" -> MyAnimeList.READING
    "watching" -> MyAnimeList.WATCHING
    "completed" -> MyAnimeList.COMPLETED
    "on_hold" -> MyAnimeList.ON_HOLD
    "dropped" -> MyAnimeList.DROPPED
    "plan_to_read" -> MyAnimeList.PLAN_TO_READ
    "plan_to_watch" -> MyAnimeList.PLAN_TO_WATCH
    else -> MyAnimeList.READING
}
