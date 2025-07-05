package eu.kanade.tachiyomi.data.track

import android.app.Application
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.track.anime.interactor.AddAnimeTracks
import eu.kanade.domain.track.anime.model.toDomainTrack
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.ImmutableList
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.anime.interactor.InsertAnimeTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack

private val addTracks: AddAnimeTracks by injectLazy()
private val insertTrack: InsertAnimeTrack by injectLazy()

interface AnimeTracker {

    // Common functions
    fun getCompletionStatus(): Long

    fun getScoreList(): ImmutableList<String>

    fun indexToScore(index: Int): Double {
        return index.toDouble()
    }

    // Anime specific functions
    fun getStatusListAnime(): List<Long>

    fun getWatchingStatus(): Long

    fun getRewatchingStatus(): Long

    // TODO: Store all scores as 10 point in the future maybe?
    fun get10PointScore(track: DomainAnimeTrack): Double {
        return track.score
    }

    fun displayScore(track: DomainAnimeTrack): String

    suspend fun update(track: AnimeTrack, didWatchEpisode: Boolean = false): AnimeTrack

    suspend fun bind(track: AnimeTrack, hasSeenEpisodes: Boolean = false): AnimeTrack

    suspend fun searchAnime(query: String): List<AnimeTrackSearch>

    suspend fun refresh(track: AnimeTrack): AnimeTrack

    // TODO: move this to an interactor, and update all trackers based on common data
    suspend fun register(item: AnimeTrack, animeId: Long) {
        item.anime_id = animeId
        try {
            addTracks.bind(this, item, animeId)
        } catch (e: Throwable) {
            withUIContext { Injekt.get<Application>().toast(e.message) }
        }
    }

    suspend fun setRemoteAnimeStatus(track: AnimeTrack, status: Long) {
        track.status = status
        if (track.status == getCompletionStatus() && track.total_episodes != 0L) {
            track.last_episode_seen = track.total_episodes.toDouble()
        }
        updateRemote(track)
    }

    suspend fun setRemoteLastEpisodeSeen(track: AnimeTrack, episodeNumber: Int) {
        if (track.last_episode_seen == 0.0 &&
            track.last_episode_seen < episodeNumber &&
            track.status != getRewatchingStatus()
        ) {
            track.status = getWatchingStatus()
        }
        track.last_episode_seen = episodeNumber.toDouble()
        if (track.total_episodes != 0L && track.last_episode_seen.toLong() == track.total_episodes) {
            track.status = getCompletionStatus()
            track.finished_watching_date = System.currentTimeMillis()
        }
        updateRemote(track)
    }

    suspend fun setRemoteScore(track: AnimeTrack, scoreString: String) {
        track.score = indexToScore(getScoreList().indexOf(scoreString))
        updateRemote(track)
    }

    suspend fun setRemoteStartDate(track: AnimeTrack, epochMillis: Long) {
        track.started_watching_date = epochMillis
        updateRemote(track)
    }

    suspend fun setRemoteFinishDate(track: AnimeTrack, epochMillis: Long) {
        track.finished_watching_date = epochMillis
        updateRemote(track)
    }

    suspend fun setRemotePrivate(track: AnimeTrack, private: Boolean) {
        track.private = private
        updateRemote(track)
    }

    private suspend fun updateRemote(track: AnimeTrack): Unit = withIOContext {
        try {
            update(track)
            track.toDomainTrack(idRequired = false)?.let {
                insertTrack.await(it)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to update remote track data id=${track.id}" }
            withUIContext { Injekt.get<Application>().toast(e.message) }
        }
    }

    fun getStatusForAnime(status: Long): StringResource?
}
