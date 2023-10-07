package eu.kanade.tachiyomi.data.track.kitsu

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
import java.text.DecimalFormat

class Kitsu(id: Long) : TrackService(id), AnimeTrackService, MangaTrackService {

    companion object {
        const val READING = 1
        const val WATCHING = 11
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 5
        const val PLAN_TO_WATCH = 15
    }

    @StringRes
    override fun nameRes() = R.string.tracker_kitsu

    override val supportsReadingDates: Boolean = true

    private val json: Json by injectLazy()

    private val interceptor by lazy { KitsuInterceptor(this) }

    private val api by lazy { KitsuApi(client, interceptor) }

    override fun getLogo() = R.drawable.ic_tracker_kitsu

    override fun getLogoColor() = Color.rgb(51, 37, 50)

    override fun getStatusListManga(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ)
    }

    override fun getStatusListAnime(): List<Int> {
        return listOf(WATCHING, PLAN_TO_WATCH, COMPLETED, ON_HOLD, DROPPED)
    }

    @StringRes
    override fun getStatus(status: Int): Int? = when (status) {
        READING -> R.string.currently_reading
        WATCHING -> R.string.currently_watching
        PLAN_TO_READ -> R.string.want_to_read
        PLAN_TO_WATCH -> R.string.want_to_watch
        COMPLETED -> R.string.completed
        ON_HOLD -> R.string.on_hold
        DROPPED -> R.string.dropped
        else -> null
    }

    override fun getReadingStatus(): Int = READING

    override fun getWatchingStatus(): Int = WATCHING

    override fun getRereadingStatus(): Int = -1

    override fun getRewatchingStatus(): Int = -1

    override fun getCompletionStatus(): Int = COMPLETED

    override fun getScoreList(): List<String> {
        val df = DecimalFormat("0.#")
        return listOf("0") + IntRange(2, 20).map { df.format(it / 2f) }
    }

    override fun indexToScore(index: Int): Float {
        return if (index > 0) (index + 1) / 2f else 0f
    }

    override fun displayScore(track: MangaTrack): String {
        val df = DecimalFormat("0.#")
        return df.format(track.score)
    }

    override fun displayScore(track: AnimeTrack): String {
        val df = DecimalFormat("0.#")
        return df.format(track.score)
    }

    private suspend fun add(track: MangaTrack): MangaTrack {
        return api.addLibManga(track, getUserId())
    }

    private suspend fun add(track: AnimeTrack): AnimeTrack {
        return api.addLibAnime(track, getUserId())
    }

    override suspend fun update(track: MangaTrack, didReadChapter: Boolean): MangaTrack {
        if (track.status != COMPLETED) {
            if (didReadChapter) {
                if (track.last_chapter_read.toInt() == track.total_chapters && track.total_chapters > 0) {
                    track.status = COMPLETED
                    track.finished_reading_date = System.currentTimeMillis()
                } else {
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
        if (track.status != COMPLETED) {
            if (didWatchEpisode) {
                if (track.last_episode_seen.toInt() == track.total_episodes && track.total_episodes > 0) {
                    track.status = COMPLETED
                    track.finished_watching_date = System.currentTimeMillis()
                } else {
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
        val remoteTrack = api.findLibManga(track, getUserId())
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.media_id = remoteTrack.media_id

            if (track.status != COMPLETED) {
                track.status = if (hasReadChapters) READING else track.status
            }

            update(track)
        } else {
            track.status = if (hasReadChapters) READING else PLAN_TO_READ
            track.score = 0F
            add(track)
        }
    }

    override suspend fun bind(track: AnimeTrack, hasReadChapters: Boolean): AnimeTrack {
        val remoteTrack = api.findLibAnime(track, getUserId())
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.media_id = remoteTrack.media_id

            if (track.status != COMPLETED) {
                track.status = if (hasReadChapters) WATCHING else track.status
            }

            update(track)
        } else {
            track.status = if (hasReadChapters) WATCHING else PLAN_TO_WATCH
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
        val remoteTrack = api.getLibManga(track)
        track.copyPersonalFrom(remoteTrack)
        track.total_chapters = remoteTrack.total_chapters
        return track
    }

    override suspend fun refresh(track: AnimeTrack): AnimeTrack {
        val remoteTrack = api.getLibAnime(track)
        track.copyPersonalFrom(remoteTrack)
        track.total_episodes = remoteTrack.total_episodes
        return track
    }

    override suspend fun login(username: String, password: String) {
        val token = api.login(username, password)
        interceptor.newAuth(token)
        val userId = api.getCurrentUser()
        saveCredentials(username, userId)
    }

    override fun logout() {
        super.logout()
        interceptor.newAuth(null)
    }

    private fun getUserId(): String {
        return getPassword()
    }

    fun saveToken(oauth: OAuth?) {
        trackPreferences.trackToken(this).set(json.encodeToString(oauth))
    }

    fun restoreToken(): OAuth? {
        return try {
            json.decodeFromString<OAuth>(trackPreferences.trackToken(this).get())
        } catch (e: Exception) {
            null
        }
    }
}
