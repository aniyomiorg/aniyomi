package tachiyomi.data.updates.anime

import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.updates.anime.model.AnimeUpdatesWithRelations

val animeUpdateWithRelationMapper: (Long, String, Long, String, String?, Boolean, Boolean, Long, Long, Long, Boolean, String?, Long, Long, Long) -> AnimeUpdatesWithRelations = {
        animeId, animeTitle, episodeId, episodeName, scanlator, seen, bookmark, lastSecondSeen, totalSeconds, sourceId, favorite, thumbnailUrl, coverLastModified, _, dateFetch ->
    AnimeUpdatesWithRelations(
        animeId = animeId,
        animeTitle = animeTitle,
        episodeId = episodeId,
        episodeName = episodeName,
        scanlator = scanlator,
        seen = seen,
        bookmark = bookmark,
        lastSecondSeen = lastSecondSeen,
        totalSeconds = totalSeconds,
        sourceId = sourceId,
        dateFetch = dateFetch,
        coverData = AnimeCover(
            animeId = animeId,
            sourceId = sourceId,
            isAnimeFavorite = favorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )
}
