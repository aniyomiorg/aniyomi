package eu.kanade.tachiyomi.data.track.myanimelist

import android.graphics.Color
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.AnimeTracker
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.DeletableAnimeTracker
import eu.kanade.tachiyomi.data.track.DeletableMangaTracker
import eu.kanade.tachiyomi.data.track.MangaTracker
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack
import tachiyomi.domain.track.manga.model.MangaTrack as DomainMangaTrack

class MyAnimeList(id: Long) :
    BaseTracker(
        id,
        "MyAnimeList",
    ),
    MangaTracker,
    AnimeTracker,
    DeletableMangaTracker,
    DeletableAnimeTracker {

    companion object {
        const val READING = 1L
        const val WATCHING = 11L
        const val COMPLETED = 2L
        const val ON_HOLD = 3L
        const val DROPPED = 4L
        const val PLAN_TO_READ = 6L
        const val PLAN_TO_WATCH = 16L
        const val REREADING = 7L
        const val REWATCHING = 17L

        private const val SEARCH_ID_PREFIX = "id:"
        private const val SEARCH_LIST_PREFIX = "my:"

        private val SCORE_LIST = IntRange(0, 10)
            .map(Int::toString)
            .toImmutableList()
    }

    private val json: Json by injectLazy()

    private val interceptor by lazy { MyAnimeListInterceptor(this) }
    private val api by lazy { MyAnimeListApi(id, client, interceptor) }

    override val supportsReadingDates: Boolean = true

    override fun getLogo() = R.drawable.ic_tracker_mal

    override fun getLogoColor() = Color.rgb(46, 81, 162)

    override fun getStatusListManga(): List<Long> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ, REREADING)
    }

    override fun getStatusListAnime(): List<Long> {
        return listOf(WATCHING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_WATCH, REWATCHING)
    }

    override fun getStatusForManga(status: Long): StringResource? = when (status) {
        READING -> MR.strings.reading
        COMPLETED -> MR.strings.completed
        ON_HOLD -> MR.strings.on_hold
        DROPPED -> MR.strings.dropped
        PLAN_TO_READ -> MR.strings.plan_to_read
        REREADING -> MR.strings.repeating
        else -> null
    }

    override fun getStatusForAnime(status: Long): StringResource? = when (status) {
        WATCHING -> AYMR.strings.watching
        COMPLETED -> MR.strings.completed
        ON_HOLD -> MR.strings.on_hold
        DROPPED -> MR.strings.dropped
        PLAN_TO_WATCH -> AYMR.strings.plan_to_watch
        REWATCHING -> AYMR.strings.repeating_anime
        else -> null
    }

    override fun getReadingStatus(): Long = READING

    override fun getWatchingStatus(): Long = WATCHING

    override fun getRereadingStatus(): Long = REREADING

    override fun getRewatchingStatus(): Long = REWATCHING

    override fun getCompletionStatus(): Long = COMPLETED

    override fun getScoreList(): ImmutableList<String> = SCORE_LIST

    override fun indexToScore(index: Int): Double {
        return index.toDouble()
    }

    override fun displayScore(track: DomainMangaTrack): String {
        return track.score.toInt().toString()
    }

    override fun displayScore(track: DomainAnimeTrack): String {
        return track.score.toInt().toString()
    }

    private suspend fun add(track: MangaTrack): MangaTrack {
        return api.updateItem(track)
    }

    private suspend fun add(track: AnimeTrack): AnimeTrack {
        return api.updateItem(track)
    }

    override suspend fun update(track: MangaTrack, didReadChapter: Boolean): MangaTrack {
        if (track.status != COMPLETED) {
            if (didReadChapter) {
                if (track.last_chapter_read.toLong() == track.total_chapters && track.total_chapters > 0) {
                    track.status = COMPLETED
                    track.finished_reading_date = System.currentTimeMillis()
                } else if (track.status != REREADING) {
                    track.status = READING
                    if (track.last_chapter_read == 1.0) {
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
                if (track.last_episode_seen.toLong() == track.total_episodes && track.total_episodes > 0) {
                    track.status = COMPLETED
                    track.finished_watching_date = System.currentTimeMillis()
                } else if (track.status != REWATCHING) {
                    track.status = WATCHING
                    if (track.last_episode_seen == 1.0) {
                        track.started_watching_date = System.currentTimeMillis()
                    }
                }
            }
        }

        return api.updateItem(track)
    }

    override suspend fun delete(track: DomainMangaTrack) {
        api.deleteMangaItem(track)
    }

    override suspend fun delete(track: DomainAnimeTrack) {
        api.deleteAnimeItem(track)
    }

    override suspend fun bind(track: MangaTrack, hasReadChapters: Boolean): MangaTrack {
        val remoteTrack = api.findListItem(track)
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.remote_id = remoteTrack.remote_id

            if (track.status != COMPLETED) {
                val isRereading = track.status == REREADING
                track.status = if (!isRereading && hasReadChapters) READING else track.status
            }

            update(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasReadChapters) READING else PLAN_TO_READ
            track.score = 0.0
            add(track)
        }
    }

    override suspend fun bind(track: AnimeTrack, hasSeenEpisodes: Boolean): AnimeTrack {
        val remoteTrack = api.findListItem(track)
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.remote_id = remoteTrack.remote_id

            if (track.status != COMPLETED) {
                val isRewatching = track.status == REWATCHING
                track.status = if (!isRewatching && hasSeenEpisodes) WATCHING else track.status
            }

            update(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasSeenEpisodes) WATCHING else PLAN_TO_WATCH
            track.score = 0.0
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
            saveCredentials(username, oauth.accessToken)
        } catch (e: Throwable) {
            logout()
        }
    }

    override fun logout() {
        super.logout()
        trackPreferences.trackToken(this).delete()
        interceptor.setAuth(null)
    }

    fun getIfAuthExpired(): Boolean {
        return trackPreferences.trackAuthExpired(this).get()
    }

    fun setAuthExpired() {
        trackPreferences.trackAuthExpired(this).set(true)
    }

    fun saveOAuth(oAuth: MALOAuth?) {
        trackPreferences.trackToken(this).set(json.encodeToString(oAuth))
    }

    fun loadOAuth(): MALOAuth? {
        return try {
            json.decodeFromString<MALOAuth>(trackPreferences.trackToken(this).get())
        } catch (e: Exception) {
            null
        }
    }
}
