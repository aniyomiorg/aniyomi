package eu.kanade.data.animeupdates

import eu.kanade.domain.anime.model.AnimeCover
import eu.kanade.domain.animeupdates.model.AnimeUpdatesWithRelations

val updateWithRelationMapper: (Long, String, Long, String, String?, Boolean, Boolean, Long, Boolean, String?, Long, Long, Long) -> AnimeUpdatesWithRelations = {
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
