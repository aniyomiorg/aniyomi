package eu.kanade.test

import android.graphics.Color
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import okhttp3.OkHttpClient

data class DummyTracker(
    override val id: Long,
    override val name: String,
    override val supportsReadingDates: Boolean = false,
    override val isLoggedIn: Boolean = false,
    val valLogoColor: Int = Color.rgb(18, 25, 35),
    val valLogo: Int = R.drawable.ic_tracker_anilist,
    val valStatuses: List<Int> = (1..6).toList(),
    val valCompletionStatus: Int = 2,
    val valScoreList: ImmutableList<String> = (0..10).map(Int::toString).toImmutableList(),
    val val10PointScore: Double = 5.4,
    val valMangaSearchResults: List<MangaTrackSearch> = listOf(),
    val valAnimeSearchResults: List<AnimeTrackSearch> = listOf(),
) : Tracker {

    override val client: OkHttpClient
        get() = TODO("Not yet implemented")

    override fun getLogoColor(): Int = valLogoColor

    override fun getLogo(): Int = valLogo

    override fun getStatus(status: Int): Int? = when (status) {
        1 -> R.string.reading
        2 -> R.string.plan_to_read
        3 -> R.string.completed
        4 -> R.string.on_hold
        5 -> R.string.dropped
        6 -> R.string.repeating
        7 -> R.string.watching
        8 -> R.string.plan_to_watch
        else -> null
    }

    override fun getCompletionStatus(): Int = valCompletionStatus

    override fun getScoreList(): ImmutableList<String> = valScoreList

    override suspend fun login(username: String, password: String) = Unit

    override fun logout() = Unit

    override fun getUsername(): String = "username"

    override fun getPassword(): String = "passw0rd"

    override fun saveCredentials(username: String, password: String) = Unit
}
