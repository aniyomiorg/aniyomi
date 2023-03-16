package eu.kanade.data.updates.anime

import eu.kanade.domain.entries.anime.model.AnimeCover
import eu.kanade.domain.updates.anime.model.AnimeUpdatesWithRelations

val animeUpdateWithRelationMapper: (Long, String, Long, String, String?, Boolean, Boolean, Long, Boolean, String?, Long, Long, Long) -> AnimeUpdatesWithRelations = {
        animeId, animeTitle, episodeId, episodeName, scanlator, seen, bookmark, sourceId, favorite, thumbnailUrl, coverLastModified, _, dateFetch ->
    AnimeUpdatesWithRelations(
        animeId = animeId,
        animeTitle = animeTitle,
        episodeId = episodeId,
        episodeName = episodeName,
        scanlator = scanlator,
        seen = seen,
        bookmark = bookmark,
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
