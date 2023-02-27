package eu.kanade.domain.items.anime.model

import eu.kanade.domain.items.CommonCover

/**
 * Contains the required data for AnimeCoverFetcher
 */
data class AnimeCover(
    val animeId: Long,
    val sourceId: Long,
    val isAnimeFavorite: Boolean,
    val url: String?,
    val lastModified: Long,
) : CommonCover

fun Anime.asAnimeCover(): AnimeCover {
    return AnimeCover(
        animeId = id,
        sourceId = source,
        isAnimeFavorite = favorite,
        url = thumbnailUrl,
        lastModified = coverLastModified,
    )
}
