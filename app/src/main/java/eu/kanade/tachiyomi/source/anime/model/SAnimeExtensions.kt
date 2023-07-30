package eu.kanade.tachiyomi.source.anime.model

import dataanime.Animes
import eu.kanade.tachiyomi.animesource.model.SAnime
import tachiyomi.domain.entries.anime.model.Anime

fun SAnime.copyFrom(other: Animes) {
    if (other.author != null) {
        author = other.author
    }

    if (other.artist != null) {
        artist = other.artist
    }

    if (other.description != null) {
        description = other.description
    }

    if (other.genre != null) {
        genre = other.genre!!.joinToString(separator = ", ")
    }

    if (other.thumbnail_url != null) {
        thumbnail_url = other.thumbnail_url
    }

    status = other.status.toInt()

    if (!initialized) {
        initialized = other.initialized
    }
}

fun Anime.copyFrom(other: Animes): Anime {
    var anime = this
    if (other.author != null) {
        anime = anime.copy(ogAuthor = other.author)
    }

    if (other.artist != null) {
        anime = anime.copy(ogArtist = other.artist)
    }

    if (other.description != null) {
        anime = anime.copy(ogDescription = other.description)
    }

    if (other.genre != null) {
        anime = anime.copy(ogGenre = other.genre)
    }

    if (other.thumbnail_url != null) {
        anime = anime.copy(thumbnailUrl = other.thumbnail_url)
    }

    anime = anime.copy(ogStatus = other.status)

    if (!initialized) {
        anime = anime.copy(initialized = other.initialized)
    }
    return anime
}
