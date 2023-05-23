package eu.kanade.domain.entries.manga.model

import tachiyomi.domain.entries.manga.model.MangaCover

fun Manga.asMangaCover(): MangaCover {
    return MangaCover(
        mangaId = id,
        sourceId = source,
        isMangaFavorite = favorite,
        url = thumbnailUrl,
        lastModified = coverLastModified,
    )
}
