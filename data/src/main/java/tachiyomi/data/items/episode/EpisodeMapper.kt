package tachiyomi.data.items.episode

import tachiyomi.domain.items.episode.model.Episode

val episodeMapper: (Long, Long, String, String, String?, Boolean, Boolean, Long, Long, Float, Long, Long, Long) -> Episode =
    { id, animeId, url, name, scanlator, seen, bookmark, lastSecondSeen, totalSeconds, episodeNumber, sourceOrder, dateFetch, dateUpload ->
        Episode(
            id = id,
            animeId = animeId,
            seen = seen,
            bookmark = bookmark,
            lastSecondSeen = lastSecondSeen,
            totalSeconds = totalSeconds,
            dateFetch = dateFetch,
            sourceOrder = sourceOrder,
            url = url,
            name = name,
            dateUpload = dateUpload,
            episodeNumber = episodeNumber,
            scanlator = scanlator,
        )
    }
