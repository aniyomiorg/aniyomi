package eu.kanade.domain.anime.model

/**
 * Contains the required data for AnimeCoverFetcher
 */
data class AnimeCover(
    val animeId: Long,
    val sourceId: Long,
    val isAnimeFavorite: Boolean,
    val url: String?,
    val lastModified: Long,
) : eu.kanade.domain.manga.model.CommonCover

fun Anime.asAnimeCover(): AnimeCover {
    return AnimeCover(
        animeId = id,
        sourceId = source,
        isAnimeFavorite = favorite,
        url = thumbnailUrl,
        lastModified = coverLastModified,
    )
}
