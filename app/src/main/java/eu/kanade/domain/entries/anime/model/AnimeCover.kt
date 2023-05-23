package eu.kanade.domain.entries.anime.model

import tachiyomi.domain.entries.anime.model.AnimeCover

fun Anime.asAnimeCover(): AnimeCover {
    return AnimeCover(
        animeId = id,
        sourceId = source,
        isAnimeFavorite = favorite,
        url = thumbnailUrl,
        lastModified = coverLastModified,
    )
}
