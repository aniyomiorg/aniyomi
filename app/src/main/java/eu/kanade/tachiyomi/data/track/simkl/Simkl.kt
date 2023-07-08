package eu.kanade.tachiyomi.data.track.simkl

import android.graphics.Color
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.track.AnimeTrackService
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.injectLazy

class Simkl(id: Long) : TrackService(id), AnimeTrackService {

    companion object {
        const val WATCHING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val NOT_INTERESTING = 4
        const val PLAN_TO_WATCH = 5
    }

    private val json: Json by injectLazy()

    private val interceptor by lazy { SimklInterceptor(this) }

    private val api by lazy { SimklApi(client, interceptor) }

    @StringRes
    override fun nameRes() = R.string.tracker_simkl

    override fun getScoreList(): List<String> {
        return IntRange(0, 10).map(Int::toString)
    }

    override fun displayScore(track: AnimeTrack): String {
        return track.score.toInt().toString()
    }

    private suspend fun add(track: AnimeTrack): AnimeTrack {
        return api.addLibAnime(track)
    }

    override suspend fun update(track: AnimeTrack, didWatchEpisode: Boolean): AnimeTrack {
        if (track.status != COMPLETED) {
            if (didWatchEpisode) {
                if (track.last_episode_seen.toInt() == track.total_episodes && track.total_episodes > 0) {
                    track.status = COMPLETED
                } else {
                    track.status = WATCHING
                }
            }
        }

        return api.updateLibAnime(track)
    }

    override suspend fun bind(track: AnimeTrack, hasSeenEpisodes: Boolean): AnimeTrack {
        val remoteTrack = api.findLibAnime(track)
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id

            if (track.status != COMPLETED) {
                track.status = if (hasSeenEpisodes) WATCHING else track.status
            }

            update(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasSeenEpisodes) WATCHING else PLAN_TO_WATCH
            track.score = 0F
            add(track)
        }
    }

    override suspend fun searchAnime(query: String): List<AnimeTrackSearch> {
        return api.searchAnime(query, "anime") +
            api.searchAnime(query, "tv") +
            api.searchAnime(query, "movie")
    }

    override suspend fun refresh(track: AnimeTrack): AnimeTrack {
        api.findLibAnime(track)?.let { remoteTrack ->
            track.copyPersonalFrom(remoteTrack)
            track.total_episodes = remoteTrack.total_episodes
        }
        return track
    }

    override fun getLogo() = R.drawable.ic_tracker_simkl

    override fun getLogoColor() = Color.rgb(0, 0, 0)

    override fun getStatusListAnime(): List<Int> {
        return listOf(WATCHING, COMPLETED, ON_HOLD, NOT_INTERESTING, PLAN_TO_WATCH)
    }

    @StringRes
    override fun getStatus(status: Int): Int? = when (status) {
        WATCHING -> R.string.watching
        PLAN_TO_WATCH -> R.string.plan_to_watch
        COMPLETED -> R.string.completed
        ON_HOLD -> R.string.on_hold
        NOT_INTERESTING -> R.string.not_interesting
        else -> null
    }

    override fun getWatchingStatus(): Int = WATCHING

    override fun getRewatchingStatus(): Int = 0

    override fun getCompletionStatus(): Int = COMPLETED

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(code: String) {
        try {
            val oauth = api.accessToken(code)
            interceptor.newAuth(oauth)
            val user = api.getCurrentUser()
            saveCredentials(user.toString(), oauth.access_token)
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
}
