package eu.kanade.tachiyomi.source.anime.model

import dataanime.Animes
import tachiyomi.domain.entries.anime.model.Anime

fun Anime.copyFrom(other: Animes): Anime {
    var anime = this
    other.author?.let { anime = anime.copy(author = it) }
    other.artist?.let { anime = anime.copy(artist = it) }
    other.description?.let { anime = anime.copy(description = it) }
    other.genre?.let { anime = anime.copy(genre = it) }
    other.thumbnail_url?.let { anime = anime.copy(thumbnailUrl = it) }
    anime = anime.copy(status = other.status)
    if (!initialized) {
        anime = anime.copy(initialized = other.initialized)
    }
    return anime
}
