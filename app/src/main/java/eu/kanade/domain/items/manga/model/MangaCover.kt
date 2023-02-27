package eu.kanade.domain.items.manga.model

import eu.kanade.domain.items.CommonCover

/**
 * Contains the required data for MangaCoverFetcher
 */
data class MangaCover(
    val mangaId: Long,
    val sourceId: Long,
    val isMangaFavorite: Boolean,
    val url: String?,
    val lastModified: Long,
) : CommonCover

fun Manga.asMangaCover(): MangaCover {
    return MangaCover(
        mangaId = id,
        sourceId = source,
        isMangaFavorite = favorite,
        url = thumbnailUrl,
        lastModified = coverLastModified,
    )
}
