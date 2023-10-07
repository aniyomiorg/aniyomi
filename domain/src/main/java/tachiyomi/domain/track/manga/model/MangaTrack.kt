package tachiyomi.domain.track.manga.model

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
)
