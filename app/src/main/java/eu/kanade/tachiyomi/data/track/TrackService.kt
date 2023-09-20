package eu.kanade.tachiyomi.data.track

import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.OkHttpClient
import uy.kohesive.injekt.injectLazy

abstract class TrackService(val id: Long) {

    val preferences: BasePreferences by injectLazy()
    val trackPreferences: TrackPreferences by injectLazy()
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

    @StringRes
    abstract fun getStatus(status: Int): Int?

    abstract suspend fun login(username: String, password: String)

    @CallSuper
    open fun logout() {
        trackPreferences.setTrackCredentials(this, "", "")
    }

    open val isLogged: Boolean
        get() = getUsername().isNotEmpty() &&
            getPassword().isNotEmpty()

    fun getUsername() = trackPreferences.trackUsername(this).get()

    fun getPassword() = trackPreferences.trackPassword(this).get()

    fun saveCredentials(username: String, password: String) {
        trackPreferences.setTrackCredentials(this, username, password)
    }

    open val animeService: AnimeTrackService
        get() = this as AnimeTrackService

    open val mangaService: MangaTrackService
        get() = this as MangaTrackService
}
