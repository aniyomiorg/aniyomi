package eu.kanade.tachiyomi.data.track

import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import eu.kanade.tachiyomi.data.track.model.TrackAnimeMetadata
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack
import tachiyomi.domain.track.manga.model.MangaTrack as DomainMangaTrack

interface Tracker {

    val id: Long

    val name: String

    val client: OkHttpClient

    // Application and remote support for reading dates
    val supportsReadingDates: Boolean

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

    suspend fun getMangaMetadata(track: DomainMangaTrack): TrackMangaMetadata?
    suspend fun getAnimeMetadata(track: DomainAnimeTrack): TrackAnimeMetadata?
}
