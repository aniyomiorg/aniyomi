package eu.kanade.tachiyomi.data.track.bangumi

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

class Bangumi(id: Long) : TrackService(id), MangaTrackService, AnimeTrackService {

    private val json: Json by injectLazy()

    private val interceptor by lazy { BangumiInterceptor(this) }

    private val api by lazy { BangumiApi(client, interceptor) }

    @StringRes
    override fun nameRes() = R.string.tracker_bangumi

    override fun getScoreList(): List<String> {
        return IntRange(0, 10).map(Int::toString)
    }

    override fun indexToScore(index: Int): Float {
        return index.toFloat()
    }

    override fun displayScore(track: MangaTrack): String {
        return track.score.toInt().toString()
    }

    override fun displayScore(track: AnimeTrack): String {
        return track.score.toInt().toString()
    }

    private suspend fun add(track: MangaTrack): MangaTrack {
        return api.addLibManga(track)
    }

    private suspend fun add(track: AnimeTrack): AnimeTrack {
        return api.addLibAnime(track)
    }

    override suspend fun update(track: MangaTrack, didReadChapter: Boolean): MangaTrack {
        if (track.status != COMPLETED) {
            if (didReadChapter) {
                if (track.last_chapter_read.toInt() == track.total_chapters && track.total_chapters > 0) {
                    track.status = COMPLETED
                } else {
                    track.status = READING
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
                } else {
                    track.status = READING
                }
            }
        }

        return api.updateLibAnime(track)
    }

    override suspend fun bind(track: MangaTrack, hasReadChapters: Boolean): MangaTrack {
        val statusTrack = api.statusLibManga(track)
        val remoteTrack = api.findLibManga(track)
        return if (remoteTrack != null && statusTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id

            if (track.status != COMPLETED) {
                track.status = if (hasReadChapters) READING else statusTrack.status
            }

            track.score = statusTrack.score
            track.last_chapter_read = statusTrack.last_chapter_read
            track.total_chapters = remoteTrack.total_chapters
            refresh(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasReadChapters) READING else PLAN_TO_READ
            track.score = 0F
            add(track)
            update(track)
        }
    }

    override suspend fun bind(track: AnimeTrack, hasSeenEpisodes: Boolean): AnimeTrack {
        val statusTrack = api.statusLibAnime(track)
        val remoteTrack = api.findLibAnime(track)
        return if (remoteTrack != null && statusTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id

            if (track.status != COMPLETED) {
                track.status = if (hasSeenEpisodes) READING else statusTrack.status
            }

            track.status = statusTrack.status
            track.score = statusTrack.score
            track.last_episode_seen = statusTrack.last_episode_seen
            track.total_episodes = remoteTrack.total_episodes
            refresh(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasSeenEpisodes) READING else PLAN_TO_READ
            track.score = 0F
            add(track)
            update(track)
        }
    }

    override suspend fun searchManga(query: String): List<MangaTrackSearch> {
        return api.search(query)
    }

    override suspend fun searchAnime(query: String): List<AnimeTrackSearch> {
        return api.searchAnime(query)
    }

    override suspend fun refresh(track: MangaTrack): MangaTrack {
        val remoteStatusTrack = api.statusLibManga(track)
        track.copyPersonalFrom(remoteStatusTrack!!)
        api.findLibManga(track)?.let { remoteTrack ->
            track.total_chapters = remoteTrack.total_chapters
        }
        return track
    }

    override suspend fun refresh(track: AnimeTrack): AnimeTrack {
        val remoteStatusTrack = api.statusLibAnime(track)
        track.copyPersonalFrom(remoteStatusTrack!!)
        api.findLibAnime(track)?.let { remoteTrack ->
            track.total_episodes = remoteTrack.total_episodes
        }
        return track
    }

    override fun getLogo() = R.drawable.ic_tracker_bangumi

    override fun getLogoColor() = Color.rgb(240, 145, 153)

    override fun getStatusListManga(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ)
    }

    override fun getStatusListAnime(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ)
    }

    @StringRes
    override fun getStatus(status: Int): Int? = when (status) {
        READING -> R.string.reading
        PLAN_TO_READ -> R.string.plan_to_read
        COMPLETED -> R.string.completed
        ON_HOLD -> R.string.on_hold
        DROPPED -> R.string.dropped
        else -> null
    }

    override fun getReadingStatus(): Int = READING

    override fun getWatchingStatus(): Int = READING

    override fun getRereadingStatus(): Int = -1

    override fun getRewatchingStatus(): Int = -1

    override fun getCompletionStatus(): Int = COMPLETED

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(code: String) {
        try {
            val oauth = api.accessToken(code)
            interceptor.newAuth(oauth)
            saveCredentials(oauth.user_id.toString(), oauth.access_token)
        } catch (e: Throwable) {
            logout()
        }
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

    override fun logout() {
        super.logout()
        trackPreferences.trackToken(this).delete()
        interceptor.newAuth(null)
    }

    companion object {
        const val READING = 3
        const val COMPLETED = 2
        const val ON_HOLD = 4
        const val DROPPED = 5
        const val PLAN_TO_READ = 1
    }
}
