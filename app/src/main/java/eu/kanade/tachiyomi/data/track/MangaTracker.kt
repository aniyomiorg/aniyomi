package eu.kanade.tachiyomi.data.track

import android.app.Application
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.track.manga.interactor.AddMangaTracks
import eu.kanade.domain.track.manga.model.toDomainTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.ImmutableList
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.manga.interactor.InsertMangaTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.manga.model.MangaTrack as DomainTrack

private val addTracks: AddMangaTracks by injectLazy()
private val insertTrack: InsertMangaTrack by injectLazy()

interface MangaTracker {

    // Common functions
    fun getCompletionStatus(): Long

    fun getScoreList(): ImmutableList<String>

    fun indexToScore(index: Int): Double {
        return index.toDouble()
    }

    // Manga specific functions
    fun getStatusListManga(): List<Long>

    fun getReadingStatus(): Long

    fun getRereadingStatus(): Long

    // TODO: Store all scores as 10 point in the future maybe?
    fun get10PointScore(track: DomainTrack): Double {
        return track.score
    }

    fun displayScore(track: DomainTrack): String

    suspend fun update(track: MangaTrack, didReadChapter: Boolean = false): MangaTrack

    suspend fun bind(track: MangaTrack, hasReadChapters: Boolean = false): MangaTrack

    suspend fun searchManga(query: String): List<MangaTrackSearch>

    suspend fun refresh(track: MangaTrack): MangaTrack

    suspend fun register(item: MangaTrack, mangaId: Long) {
        item.manga_id = mangaId
        try {
            addTracks.bind(this, item, mangaId)
        } catch (e: Throwable) {
            withUIContext { Injekt.get<Application>().toast(e.message) }
        }
    }

    suspend fun setRemoteMangaStatus(track: MangaTrack, status: Long) {
        track.status = status
        if (track.status == getCompletionStatus() && track.total_chapters != 0L) {
            track.last_chapter_read = track.total_chapters.toDouble()
        }
        updateRemote(track)
    }

    suspend fun setRemoteLastChapterRead(track: MangaTrack, chapterNumber: Int) {
        if (track.last_chapter_read == 0.0 &&
            track.last_chapter_read < chapterNumber &&
            track.status != getRereadingStatus()
        ) {
            track.status = getReadingStatus()
        }
        track.last_chapter_read = chapterNumber.toDouble()
        if (track.total_chapters != 0L &&
            track.last_chapter_read.toLong() == track.total_chapters
        ) {
            track.status = getCompletionStatus()
            track.finished_reading_date = System.currentTimeMillis()
        }
        updateRemote(track)
    }

    suspend fun setRemoteScore(track: MangaTrack, scoreString: String) {
        track.score = indexToScore(getScoreList().indexOf(scoreString))
        updateRemote(track)
    }

    suspend fun setRemoteStartDate(track: MangaTrack, epochMillis: Long) {
        track.started_reading_date = epochMillis
        updateRemote(track)
    }

    suspend fun setRemoteFinishDate(track: MangaTrack, epochMillis: Long) {
        track.finished_reading_date = epochMillis
        updateRemote(track)
    }

    suspend fun setRemotePrivate(track: MangaTrack, private: Boolean) {
        track.private = private
        updateRemote(track)
    }

    private suspend fun updateRemote(track: MangaTrack): Unit = withIOContext {
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

    fun getStatusForManga(status: Long): StringResource?
}
