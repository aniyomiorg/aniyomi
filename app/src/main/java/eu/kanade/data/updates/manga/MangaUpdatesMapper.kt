package eu.kanade.data.updates.manga

import eu.kanade.domain.entries.manga.model.MangaCover
import eu.kanade.domain.updates.manga.model.MangaUpdatesWithRelations

val mangaUpdateWithRelationMapper: (Long, String, Long, String, String?, Boolean, Boolean, Long, Boolean, String?, Long, Long, Long) -> MangaUpdatesWithRelations = {
        mangaId, mangaTitle, chapterId, chapterName, scanlator, read, bookmark, sourceId, favorite, thumbnailUrl, coverLastModified, _, dateFetch ->
    MangaUpdatesWithRelations(
        mangaId = mangaId,
        mangaTitle = mangaTitle,
        chapterId = chapterId,
        chapterName = chapterName,
        scanlator = scanlator,
        read = read,
        bookmark = bookmark,
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
