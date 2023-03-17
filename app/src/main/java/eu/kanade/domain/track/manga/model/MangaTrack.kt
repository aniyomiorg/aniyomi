package eu.kanade.domain.track.manga.model

import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack as DbMangaTrack

data class MangaTrack(
    val id: Long,
    val mangaId: Long,
    val syncId: Long,
    val remoteId: Long,
    val libraryId: Long?,
    val title: String,
    val lastChapterRead: Double,
    val totalChapters: Long,
    val status: Long,
    val score: Float,
    val remoteUrl: String,
    val startDate: Long,
    val finishDate: Long,
) {
    fun copyPersonalFrom(other: MangaTrack): MangaTrack {
        return this.copy(
            lastChapterRead = other.lastChapterRead,
            score = other.score,
            status = other.status,
            startDate = other.startDate,
            finishDate = other.finishDate,
        )
    }
}

fun MangaTrack.toDbTrack(): DbMangaTrack = eu.kanade.tachiyomi.data.database.models.manga.MangaTrack.create(syncId).also {
    it.id = id
    it.manga_id = mangaId
    it.media_id = remoteId
    it.library_id = libraryId
    it.title = title
    it.last_chapter_read = lastChapterRead.toFloat()
    it.total_chapters = totalChapters.toInt()
    it.status = status.toInt()
    it.score = score
    it.tracking_url = remoteUrl
    it.started_reading_date = startDate
    it.finished_reading_date = finishDate
}

fun DbMangaTrack.toDomainTrack(idRequired: Boolean = true): MangaTrack? {
    val trackId = id ?: if (idRequired.not()) -1 else return null
    return MangaTrack(
        id = trackId,
        mangaId = manga_id,
        syncId = sync_id.toLong(),
        remoteId = media_id,
        libraryId = library_id,
        title = title,
        lastChapterRead = last_chapter_read.toDouble(),
        totalChapters = total_chapters.toLong(),
        status = status.toLong(),
        score = score,
        remoteUrl = tracking_url,
        startDate = started_reading_date,
        finishDate = finished_reading_date,
    )
}
