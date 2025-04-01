package eu.kanade.tachiyomi.data.track

import androidx.annotation.CallSuper
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import okhttp3.OkHttpClient
import uy.kohesive.injekt.injectLazy

abstract class BaseTracker(
    override val id: Long,
    override val name: String,
) : Tracker {

    val trackPreferences: TrackPreferences by injectLazy()
    val networkService: NetworkHelper by injectLazy()

    override val client: OkHttpClient
        get() = networkService.client

    // Application and remote support for reading dates
    override val supportsReadingDates: Boolean = false

    override val supportsPrivateTracking: Boolean = false

    @CallSuper
    override fun logout() {
        trackPreferences.setCredentials(this, "", "")
    }

    override val isLoggedIn: Boolean
        get() = getUsername().isNotEmpty() &&
            getPassword().isNotEmpty()

    override val isLoggedInFlow: Flow<Boolean> by lazy {
        combine(
            trackPreferences.trackUsername(this).changes(),
            trackPreferences.trackPassword(this).changes(),
        ) { username, password ->
            username.isNotEmpty() && password.isNotEmpty()
        }
    }

    override fun getUsername() = trackPreferences.trackUsername(this).get()

    override fun getPassword() = trackPreferences.trackPassword(this).get()

    override fun saveCredentials(username: String, password: String) {
        trackPreferences.setCredentials(this, username, password)
    }
}
