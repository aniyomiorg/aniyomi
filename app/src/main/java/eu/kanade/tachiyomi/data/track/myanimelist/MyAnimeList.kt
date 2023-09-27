package eu.kanade.tachiyomi.data.track.myanimelist

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

class MyAnimeList(id: Long) : TrackService(id), MangaTrackService, AnimeTrackService {

    companion object {
        const val READING = 1
        const val WATCHING = 11
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 6
        const val PLAN_TO_WATCH = 16
        const val REREADING = 7
        const val REWATCHING = 17

        private const val SEARCH_ID_PREFIX = "id:"
        private const val SEARCH_LIST_PREFIX = "my:"
    }

    private val json: Json by injectLazy()

    private val interceptor by lazy { MyAnimeListInterceptor(this, getPassword()) }
    private val api by lazy { MyAnimeListApi(client, interceptor) }

    @StringRes
    override fun nameRes() = R.string.tracker_myanimelist

    override val supportsReadingDates: Boolean = true

    override fun getLogo() = R.drawable.ic_tracker_mal

    override fun getLogoColor() = Color.rgb(46, 81, 162)

    override fun getStatusListManga(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ, REREADING)
    }

    override fun getStatusListAnime(): List<Int> {
        return listOf(WATCHING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_WATCH, REWATCHING)
    }

    @StringRes
    override fun getStatus(status: Int): Int? = when (status) {
        READING -> R.string.reading
        WATCHING -> R.string.watching
        COMPLETED -> R.string.completed
        ON_HOLD -> R.string.on_hold
        DROPPED -> R.string.dropped
        PLAN_TO_READ -> R.string.plan_to_read
        PLAN_TO_WATCH -> R.string.plan_to_watch
        REREADING -> R.string.repeating
        REWATCHING -> R.string.repeating_anime
        else -> null
    }

    override fun getReadingStatus(): Int = READING

    override fun getWatchingStatus(): Int = WATCHING

    override fun getRereadingStatus(): Int = REREADING

    override fun getRewatchingStatus(): Int = REWATCHING

    override fun getCompletionStatus(): Int = COMPLETED

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
        return api.updateItem(track)
    }

    private suspend fun add(track: AnimeTrack): AnimeTrack {
        track.status = WATCHING
        track.score = 0F
        return api.updateItem(track)
    }

    override suspend fun update(track: MangaTrack, didReadChapter: Boolean): MangaTrack {
        if (track.status != COMPLETED) {
            if (didReadChapter) {
                if (track.last_chapter_read.toInt() == track.total_chapters && track.total_chapters > 0) {
                    track.status = COMPLETED
                    track.finished_reading_date = System.currentTimeMillis()
                } else if (track.status != REREADING) {
                    track.status = READING
                    if (track.last_chapter_read == 1F) {
                        track.started_reading_date = System.currentTimeMillis()
                    }
                }
            }
        }

        return api.updateItem(track)
    }

    override suspend fun update(track: AnimeTrack, didWatchEpisode: Boolean): AnimeTrack {
        if (track.status != COMPLETED) {
            if (didWatchEpisode) {
                if (track.last_episode_seen.toInt() == track.total_episodes && track.total_episodes > 0) {
                    track.status = COMPLETED
                    track.finished_watching_date = System.currentTimeMillis()
                } else if (track.status != REWATCHING) {
                    track.status = WATCHING
                    if (track.last_episode_seen == 1F) {
                        track.started_watching_date = System.currentTimeMillis()
                    }
                }
            }
        }

        return api.updateItem(track)
    }

    override suspend fun bind(track: MangaTrack, hasReadChapters: Boolean): MangaTrack {
        val remoteTrack = api.findListItem(track)
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.media_id = remoteTrack.media_id

            if (track.status != COMPLETED) {
                val isRereading = track.status == REREADING
                track.status = if (isRereading.not() && hasReadChapters) READING else track.status
            }

            update(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasReadChapters) READING else PLAN_TO_READ
            track.score = 0F
            add(track)
        }
    }

    override suspend fun bind(track: AnimeTrack, hasReadChapters: Boolean): AnimeTrack {
        val remoteTrack = api.findListItem(track)
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.media_id = remoteTrack.media_id

            if (track.status != COMPLETED) {
                val isRereading = track.status == REWATCHING
                track.status = if (isRereading.not() && hasReadChapters) WATCHING else track.status
            }

            update(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasReadChapters) WATCHING else PLAN_TO_WATCH
            track.score = 0F
            add(track)
        }
    }

    override suspend fun searchManga(query: String): List<MangaTrackSearch> {
        if (query.startsWith(SEARCH_ID_PREFIX)) {
            query.substringAfter(SEARCH_ID_PREFIX).toIntOrNull()?.let { id ->
                return listOf(api.getMangaDetails(id))
            }
        }

        if (query.startsWith(SEARCH_LIST_PREFIX)) {
            query.substringAfter(SEARCH_LIST_PREFIX).let { title ->
                return api.findListItems(title)
            }
        }

        return api.search(query)
    }

    override suspend fun searchAnime(query: String): List<AnimeTrackSearch> {
        if (query.startsWith(SEARCH_ID_PREFIX)) {
            query.substringAfter(SEARCH_ID_PREFIX).toIntOrNull()?.let { id ->
                return listOf(api.getAnimeDetails(id))
            }
        }

        if (query.startsWith(SEARCH_LIST_PREFIX)) {
            query.substringAfter(SEARCH_LIST_PREFIX).let { title ->
                return api.findListItemsAnime(title)
            }
        }

        return api.searchAnime(query)
    }

    override suspend fun refresh(track: MangaTrack): MangaTrack {
        return api.findListItem(track) ?: add(track)
    }

    override suspend fun refresh(track: AnimeTrack): AnimeTrack {
        return api.findListItem(track) ?: add(track)
    }

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(authCode: String) {
        try {
            val oauth = api.getAccessToken(authCode)
            interceptor.setAuth(oauth)
            val username = api.getCurrentUser()
            saveCredentials(username, oauth.access_token)
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
