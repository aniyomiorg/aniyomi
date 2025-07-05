package eu.kanade.tachiyomi.data.track

import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient

interface Tracker {

    val id: Long

    val name: String

    val client: OkHttpClient

    // Application and remote support for reading dates
    val supportsReadingDates: Boolean

    val supportsPrivateTracking: Boolean

    @DrawableRes
    fun getLogo(): Int

    @ColorInt
    fun getLogoColor(): Int

    fun getCompletionStatus(): Long

    fun getScoreList(): ImmutableList<String>

    suspend fun login(username: String, password: String)

    @CallSuper
    fun logout()

    val isLoggedIn: Boolean

    val isLoggedInFlow: Flow<Boolean>

    fun getUsername(): String

    fun getPassword(): String

    fun saveCredentials(username: String, password: String)

    val animeService: AnimeTracker
        get() = this as AnimeTracker

    val mangaService: MangaTracker
        get() = this as MangaTracker
}
