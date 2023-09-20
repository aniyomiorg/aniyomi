package tachiyomi.data.track.anime

import tachiyomi.domain.track.anime.model.AnimeTrack

val animeTrackMapper: (Long, Long, Long, Long, Long?, String, Double, Long, Long, Float, String, Long, Long) -> AnimeTrack =
    { id, animeId, syncId, remoteId, libraryId, title, lastEpisodeSeen, totalEpisodes, status, score, remoteUrl, startDate, finishDate ->
        AnimeTrack(
            id = id,
            animeId = animeId,
            syncId = syncId,
            remoteId = remoteId,
            libraryId = libraryId,
            title = title,
            lastEpisodeSeen = lastEpisodeSeen,
            totalEpisodes = totalEpisodes,
            status = status,
            score = score,
            remoteUrl = remoteUrl,
            startDate = startDate,
            finishDate = finishDate,
        )
    }
