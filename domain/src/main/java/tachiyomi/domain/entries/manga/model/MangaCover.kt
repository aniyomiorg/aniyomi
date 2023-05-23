package tachiyomi.domain.entries.manga.model

import tachiyomi.domain.entries.EntryCover

/**
 * Contains the required data for MangaCoverFetcher
 */
data class MangaCover(
    val mangaId: Long,
    val sourceId: Long,
    val isMangaFavorite: Boolean,
    val url: String?,
    val lastModified: Long,
) : EntryCover
