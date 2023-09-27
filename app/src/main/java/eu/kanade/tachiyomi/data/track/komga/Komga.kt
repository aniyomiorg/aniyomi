package eu.kanade.tachiyomi.data.track.komga

import android.content.Context
import android.graphics.Color
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.EnhancedMangaTrackService
import eu.kanade.tachiyomi.data.track.MangaTrackService
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.source.MangaSource
import okhttp3.Dns
import okhttp3.OkHttpClient
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.track.manga.model.MangaTrack as DomainTrack

class Komga(private val context: Context, id: Long) : TrackService(id), EnhancedMangaTrackService, MangaTrackService {

    companion object {
        const val UNREAD = 1
        const val READING = 2
        const val COMPLETED = 3
    }

    override val client: OkHttpClient =
        networkService.client.newBuilder()
            .dns(Dns.SYSTEM) // don't use DNS over HTTPS as it breaks IP addressing
            .build()

    val api by lazy { KomgaApi(client) }

    @StringRes
    override fun nameRes() = R.string.tracker_komga

    override fun getLogo() = R.drawable.ic_tracker_komga

    override fun getLogoColor() = Color.rgb(51, 37, 50)

    override fun getStatusListManga() = listOf(UNREAD, READING, COMPLETED)

    @StringRes
    override fun getStatus(status: Int): Int? = when (status) {
        UNREAD -> R.string.unread
        READING -> R.string.reading
        COMPLETED -> R.string.completed
        else -> null
    }

    override fun getReadingStatus(): Int = READING

    override fun getRereadingStatus(): Int = -1

    override fun displayScore(track: MangaTrack): String = ""

    override fun getCompletionStatus(): Int = COMPLETED

    override fun getScoreList(): List<String> = emptyList()

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

        return api.updateProgress(track)
    }

    override suspend fun bind(track: MangaTrack, hasReadChapters: Boolean): MangaTrack {
        return track
    }

    override suspend fun searchManga(query: String): List<MangaTrackSearch> = throw Exception("Not used")

    override suspend fun refresh(track: MangaTrack): MangaTrack {
        val remoteTrack = api.getTrackSearch(track.tracking_url)
        track.copyPersonalFrom(remoteTrack)
        track.total_chapters = remoteTrack.total_chapters
        return track
    }

    override suspend fun login(username: String, password: String) {
        saveCredentials("user", "pass")
    }

    // TrackService.isLogged works by checking that credentials are saved.
    // By saving dummy, unused credentials, we can activate the tracker simply by login/logout
    override fun loginNoop() {
        saveCredentials("user", "pass")
    }

    override fun getAcceptedSources() = listOf("eu.kanade.tachiyomi.extension.all.komga.Komga")

    override suspend fun match(manga: Manga): MangaTrackSearch? =
        try {
            api.getTrackSearch(manga.url)
        } catch (e: Exception) {
            null
        }

    override fun isTrackFrom(track: DomainTrack, manga: Manga, source: MangaSource?): Boolean =
        track.remoteUrl == manga.url && source?.let { accept(it) } == true

    override fun migrateTrack(track: DomainTrack, manga: Manga, newSource: MangaSource): DomainTrack? =
        if (accept(newSource)) {
            track.copy(remoteUrl = manga.url)
        } else {
            null
        }
}
