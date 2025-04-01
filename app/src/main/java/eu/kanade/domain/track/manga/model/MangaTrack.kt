package eu.kanade.domain.track.manga.model

import tachiyomi.domain.track.manga.model.MangaTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack as DbMangaTrack

fun MangaTrack.copyPersonalFrom(other: MangaTrack): MangaTrack {
    return this.copy(
        lastChapterRead = other.lastChapterRead,
        score = other.score,
        status = other.status,
        startDate = other.startDate,
        finishDate = other.finishDate,
        private = other.private,
    )
}

fun MangaTrack.toDbTrack(): DbMangaTrack = DbMangaTrack.create(trackerId).also {
    it.id = id
    it.manga_id = mangaId
    it.remote_id = remoteId
    it.library_id = libraryId
    it.title = title
    it.last_chapter_read = lastChapterRead
    it.total_chapters = totalChapters
    it.status = status
    it.score = score
    it.tracking_url = remoteUrl
    it.started_reading_date = startDate
    it.finished_reading_date = finishDate
    it.private = private
}

fun DbMangaTrack.toDomainTrack(idRequired: Boolean = true): MangaTrack? {
    val trackId = id ?: if (!idRequired) -1 else return null
    return MangaTrack(
        id = trackId,
        mangaId = manga_id,
        trackerId = tracker_id,
        remoteId = remote_id,
        libraryId = library_id,
        title = title,
        lastChapterRead = last_chapter_read,
        totalChapters = total_chapters,
        status = status,
        score = score,
        remoteUrl = tracking_url,
        startDate = started_reading_date,
        finishDate = finished_reading_date,
        private = private,
    )
}
