package eu.kanade.tachiyomi.data.track.anilist

import android.graphics.Color
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.AnimeTrackService
import eu.kanade.tachiyomi.data.track.MangaTrackService
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack
import tachiyomi.domain.track.manga.model.MangaTrack as DomainTrack

class Anilist(id: Long) : TrackService(id), MangaTrackService, AnimeTrackService {

    companion object {
        const val READING = 1
        const val WATCHING = 11
        const val COMPLETED = 2
        const val PAUSED = 3
        const val DROPPED = 4
        const val PLANNING = 5
        const val PLANNING_ANIME = 15
        const val REPEATING = 6
        const val REPEATING_ANIME = 16

        const val POINT_100 = "POINT_100"
        const val POINT_10 = "POINT_10"
        const val POINT_10_DECIMAL = "POINT_10_DECIMAL"
        const val POINT_5 = "POINT_5"
        const val POINT_3 = "POINT_3"
    }

    private val json: Json by injectLazy()

    private val interceptor by lazy { AnilistInterceptor(this, getPassword()) }

    private val api by lazy { AnilistApi(client, interceptor) }

    override val supportsReadingDates: Boolean = true

    private val scorePreference = trackPreferences.anilistScoreType()

    init {
        // If the preference is an int from APIv1, logout user to force using APIv2
        try {
            scorePreference.get()
        } catch (e: ClassCastException) {
            logout()
            scorePreference.delete()
        }
    }

    @StringRes
    override fun nameRes() = R.string.tracker_anilist

    override fun getLogo() = R.drawable.ic_tracker_anilist

    override fun getLogoColor() = Color.rgb(18, 25, 35)

    override fun getStatusListManga(): List<Int> {
        return listOf(READING, PLANNING, COMPLETED, REPEATING, PAUSED, DROPPED)
    }

    override fun getStatusListAnime(): List<Int> {
        return listOf(WATCHING, PLANNING_ANIME, COMPLETED, REPEATING_ANIME, PAUSED, DROPPED)
    }

    @StringRes
    override fun getStatus(status: Int): Int? = when (status) {
        WATCHING -> R.string.watching
        READING -> R.string.reading
        PLANNING -> R.string.plan_to_read
        PLANNING_ANIME -> R.string.plan_to_watch
        COMPLETED -> R.string.completed
        REPEATING -> R.string.repeating
        REPEATING_ANIME -> R.string.repeating_anime
        PAUSED -> R.string.paused
        DROPPED -> R.string.dropped
        else -> null
    }

    override fun getReadingStatus(): Int = READING

    override fun getWatchingStatus(): Int = WATCHING

    override fun getRereadingStatus(): Int = REPEATING

    override fun getRewatchingStatus(): Int = REPEATING_ANIME

    override fun getCompletionStatus(): Int = COMPLETED

    override fun getScoreList(): List<String> {
        return when (scorePreference.get()) {
            // 10 point
            POINT_10 -> IntRange(0, 10).map(Int::toString)
            // 100 point
            POINT_100 -> IntRange(0, 100).map(Int::toString)
            // 5 stars
            POINT_5 -> IntRange(0, 5).map { "$it ★" }
            // Smiley
            POINT_3 -> listOf("-", "😦", "😐", "😊")
            // 10 point decimal
            POINT_10_DECIMAL -> IntRange(0, 100).map { (it / 10f).toString() }
            else -> throw Exception("Unknown score type")
        }
    }

    override fun get10PointScore(track: DomainTrack): Float {
        // Score is stored in 100 point format
        return track.score / 10f
    }

    override fun get10PointScore(track: DomainAnimeTrack): Float {
        // Score is stored in 100 point format
        return track.score / 10f
    }

    override fun indexToScore(index: Int): Float {
        return when (scorePreference.get()) {
            // 10 point
            POINT_10 -> index * 10f
            // 100 point
            POINT_100 -> index.toFloat()
            // 5 stars
            POINT_5 -> when (index) {
                0 -> 0f
                else -> index * 20f - 10f
            }
            // Smiley
            POINT_3 -> when (index) {
                0 -> 0f
                else -> index * 25f + 10f
            }
            // 10 point decimal
            POINT_10_DECIMAL -> index.toFloat()
            else -> throw Exception("Unknown score type")
        }
    }

    override fun displayScore(track: MangaTrack): String {
        val score = track.score

        return when (scorePreference.get()) {
            POINT_5 -> when (score) {
                0f -> "0 ★"
                else -> "${((score + 10) / 20).toInt()} ★"
            }
            POINT_3 -> when {
                score == 0f -> "0"
                score <= 35 -> "😦"
                score <= 60 -> "😐"
                else -> "😊"
            }
            else -> track.toAnilistScore()
        }
    }

    override fun displayScore(track: AnimeTrack): String {
        val score = track.score

        return when (scorePreference.get()) {
            POINT_5 -> when (score) {
                0f -> "0 ★"
                else -> "${((score + 10) / 20).toInt()} ★"
            }
            POINT_3 -> when {
                score == 0f -> "0"
                score <= 35 -> "😦"
                score <= 60 -> "😐"
                else -> "😊"
            }
            else -> track.toAnilistScore()
        }
    }

    private suspend fun add(track: MangaTrack): MangaTrack {
        return api.addLibManga(track)
    }

    private suspend fun add(track: AnimeTrack): AnimeTrack {
        return api.addLibAnime(track)
    }

    override suspend fun update(track: MangaTrack, didReadChapter: Boolean): MangaTrack {
        // If user was using API v1 fetch library_id
        if (track.library_id == null || track.library_id!! == 0L) {
            val libManga = api.findLibManga(track, getUsername().toInt())
                ?: throw Exception("$track not found on user library")
            track.library_id = libManga.library_id
        }

        if (track.status != COMPLETED) {
            if (didReadChapter) {
                if (track.last_chapter_read.toInt() == track.total_chapters && track.total_chapters > 0) {
                    track.status = COMPLETED
                    track.finished_reading_date = System.currentTimeMillis()
                } else if (track.status != REPEATING) {
                    track.status = READING
                    if (track.last_chapter_read == 1F) {
                        track.started_reading_date = System.currentTimeMillis()
                    }
                }
            }
        }

        return api.updateLibManga(track)
    }

    override suspend fun update(track: AnimeTrack, didWatchEpisode: Boolean): AnimeTrack {
        // If user was using API v1 fetch library_id
        if (track.library_id == null || track.library_id!! == 0L) {
            val libManga = api.findLibAnime(track, getUsername().toInt())
                ?: throw Exception("$track not found on user library")
            track.library_id = libManga.library_id
        }

        if (track.status != COMPLETED) {
            if (didWatchEpisode) {
                if (track.last_episode_seen.toInt() == track.total_episodes && track.total_episodes > 0) {
                    track.status = COMPLETED
                    track.finished_watching_date = System.currentTimeMillis()
                } else if (track.status != REPEATING_ANIME) {
                    track.status = WATCHING
                    if (track.last_episode_seen == 1F) {
                        track.started_watching_date = System.currentTimeMillis()
                    }
                }
            }
        }

        return api.updateLibAnime(track)
    }

    override suspend fun bind(track: MangaTrack, hasReadChapters: Boolean): MangaTrack {
        val remoteTrack = api.findLibManga(track, getUsername().toInt())
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id

            if (track.status != COMPLETED) {
                val isRereading = track.status == REPEATING
                track.status = if (isRereading.not() && hasReadChapters) READING else track.status
            }

            update(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasReadChapters) READING else PLANNING
            track.score = 0F
            add(track)
        }
    }

    override suspend fun bind(track: AnimeTrack, hasReadChapters: Boolean): AnimeTrack {
        val remoteTrack = api.findLibAnime(track, getUsername().toInt())
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id

            if (track.status != COMPLETED) {
                val isRereading = track.status == REPEATING_ANIME
                track.status = if (isRereading.not() && hasReadChapters) WATCHING else track.status
            }

            update(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasReadChapters) WATCHING else PLANNING_ANIME
            track.score = 0F
            add(track)
        }
    }

    override suspend fun searchManga(query: String): List<MangaTrackSearch> {
        return api.search(query)
    }

    override suspend fun searchAnime(query: String): List<AnimeTrackSearch> {
        return api.searchAnime(query)
    }

    override suspend fun refresh(track: MangaTrack): MangaTrack {
        val remoteTrack = api.getLibManga(track, getUsername().toInt())
        track.copyPersonalFrom(remoteTrack)
        track.title = remoteTrack.title
        track.total_chapters = remoteTrack.total_chapters
        return track
    }

    override suspend fun refresh(track: AnimeTrack): AnimeTrack {
        val remoteTrack = api.getLibAnime(track, getUsername().toInt())
        track.copyPersonalFrom(remoteTrack)
        track.title = remoteTrack.title
        track.total_episodes = remoteTrack.total_episodes
        return track
    }

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(token: String) {
        try {
            val oauth = api.createOAuth(token)
            interceptor.setAuth(oauth)
            val (username, scoreType) = api.getCurrentUser()
            scorePreference.set(scoreType)
            saveCredentials(username.toString(), oauth.access_token)
        } catch (e: Throwable) {
            logout()
        }
    }

    override fun logout() {
        super.logout()
        trackPreferences.trackToken(this).delete()
        interceptor.setAuth(null)
    }

    fun saveOAuth(oAuth: OAuth?) {
        trackPreferences.trackToken(this).set(json.encodeToString(oAuth))
    }

    fun loadOAuth(): OAuth? {
        return try {
            json.decodeFromString<OAuth>(trackPreferences.trackToken(this).get())
        } catch (e: Exception) {
            null
        }
    }
}
