package eu.kanade.tachiyomi.source.manga.model

import data.Mangas
import tachiyomi.domain.entries.manga.model.Manga

fun Manga.copyFrom(other: Mangas): Manga {
    var manga = this
    other.author?.let { manga = manga.copy(ogAuthor = it) }
    other.artist?.let { manga = manga.copy(ogArtist = it) }
    other.description?.let { manga = manga.copy(ogDescription = it) }
    other.genre?.let { manga = manga.copy(ogGenre = it) }
    other.thumbnail_url?.let { manga = manga.copy(thumbnailUrl = it) }
    manga = manga.copy(ogStatus = other.status)
    if (!initialized) {
        manga = manga.copy(initialized = other.initialized)
    }
    return manga
}
