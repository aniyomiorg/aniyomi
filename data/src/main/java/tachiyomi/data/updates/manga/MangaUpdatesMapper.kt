package tachiyomi.data.updates.manga

import tachiyomi.domain.entries.manga.model.MangaCover
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations

val mangaUpdateWithRelationMapper: (Long, String, Long, String, String?, Boolean, Boolean, Long, Long, Boolean, String?, Long, Long, Long) -> MangaUpdatesWithRelations = {
        mangaId, mangaTitle, chapterId, chapterName, scanlator, read, bookmark, lastPageRead, sourceId, favorite, thumbnailUrl, coverLastModified, _, dateFetch ->
    MangaUpdatesWithRelations(
        mangaId = mangaId,
        mangaTitle = mangaTitle,
        chapterId = chapterId,
        chapterName = chapterName,
        scanlator = scanlator,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        sourceId = sourceId,
        dateFetch = dateFetch,
        coverData = MangaCover(
            mangaId = mangaId,
            sourceId = sourceId,
            isMangaFavorite = favorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )
}
