package tachiyomi.data.history.manga

import java.util.Date
import tachiyomi.domain.entries.manga.model.MangaCover
import tachiyomi.domain.history.manga.model.MangaHistory
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations

val mangaHistoryMapper: (Long, Long, Date?, Long) -> MangaHistory = { id, chapterId, readAt, readDuration ->
    MangaHistory(
        id = id,
        chapterId = chapterId,
        readAt = readAt,
        readDuration = readDuration,
    )
}

val mangaHistoryWithRelationsMapper: (
    Long,
    Long,
    Long,
    String,
    String?,
    Long,
    Boolean,
    Long,
    Double,
    Date?,
    Long
) -> MangaHistoryWithRelations = {
        historyId, mangaId, chapterId, title, thumbnailUrl, sourceId, isFavorite, coverLastModified, chapterNumber, readAt, readDuration ->
    MangaHistoryWithRelations(
        id = historyId,
        chapterId = chapterId,
        mangaId = mangaId,
        title = title,
        chapterNumber = chapterNumber,
        readAt = readAt,
        readDuration = readDuration,
        coverData = MangaCover(
            mangaId = mangaId,
            sourceId = sourceId,
            isMangaFavorite = isFavorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )
}
