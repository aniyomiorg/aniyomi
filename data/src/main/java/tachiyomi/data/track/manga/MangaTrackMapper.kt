package tachiyomi.data.track.manga

import tachiyomi.domain.track.manga.model.MangaTrack

val mangaTrackMapper: (Long, Long, Long, Long, Long?, String, Double, Long, Long, Float, String, Long, Long) -> MangaTrack =
    { id, mangaId, syncId, remoteId, libraryId, title, lastChapterRead, totalChapters, status, score, remoteUrl, startDate, finishDate ->
        MangaTrack(
            id = id,
            mangaId = mangaId,
            syncId = syncId,
            remoteId = remoteId,
            libraryId = libraryId,
            title = title,
            lastChapterRead = lastChapterRead,
            totalChapters = totalChapters,
            status = status,
            score = score,
            remoteUrl = remoteUrl,
            startDate = startDate,
            finishDate = finishDate,
        )
    }
