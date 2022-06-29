package eu.kanade.tachiyomi.data.track

import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.OkHttpClient
import uy.kohesive.injekt.injectLazy

abstract class TrackService(val id: Long) {

    val preferences: PreferencesHelper by injectLazy()
    val networkService: NetworkHelper by injectLazy()

    open val client: OkHttpClient
        get() = networkService.client

    // Name of the manga sync service to display
    @StringRes
    abstract fun nameRes(): Int

    // Application and remote support for reading dates
    open val supportsReadingDates: Boolean = false

    @DrawableRes
    abstract fun getLogo(): Int

    @ColorInt
    abstract fun getLogoColor(): Int

    abstract fun getStatusList(): List<Int>

    abstract fun getStatusListAnime(): List<Int>

    abstract fun getStatus(status: Int): String

    abstract fun getReadingStatus(): Int

    abstract fun getWatchingStatus(): Int

    abstract fun getRereadingStatus(): Int

    abstract fun getRewatchingStatus(): Int

    abstract fun getCompletionStatus(): Int

    abstract fun getScoreList(): List<String>

    open fun indexToScore(index: Int): Float {
        return index.toFloat()
    }

    abstract fun displayScore(track: Track): String

    abstract fun displayScore(track: AnimeTrack): String

    abstract suspend fun update(track: Track, didReadChapter: Boolean = false): Track

    abstract suspend fun update(track: AnimeTrack, didWatchEpisode: Boolean = false): AnimeTrack

    abstract suspend fun bind(track: Track, hasReadChapters: Boolean = false): Track

    abstract suspend fun bind(track: AnimeTrack, hasReadChapters: Boolean = false): AnimeTrack

    abstract suspend fun search(query: String): List<TrackSearch>

    abstract suspend fun searchAnime(query: String): List<AnimeTrackSearch>

    abstract suspend fun refresh(track: Track): Track

    abstract suspend fun refresh(track: AnimeTrack): AnimeTrack

    abstract suspend fun login(username: String, password: String)

    @CallSuper
    open fun logout() {
        preferences.setTrackCredentials(this, "", "")
    }

    open val isLogged: Boolean
        get() = getUsername().isNotEmpty() &&
            getPassword().isNotEmpty()

    fun getUsername() = preferences.trackUsername(this)!!

    fun getPassword() = preferences.trackPassword(this)!!

    fun saveCredentials(username: String, password: String) {
        preferences.setTrackCredentials(this, username, password)
    }
}
