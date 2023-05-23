package tachiyomi.domain.entries.anime.model

import tachiyomi.domain.entries.EntryCover

/**
 * Contains the required data for AnimeCoverFetcher
 */
data class AnimeCover(
    val animeId: Long,
    val sourceId: Long,
    val isAnimeFavorite: Boolean,
    val url: String?,
    val lastModified: Long,
) : EntryCover
