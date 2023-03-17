package eu.kanade.tachiyomi.source.anime.model

import dataanime.Animes
import eu.kanade.tachiyomi.animesource.model.SAnime

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
        genre = other.genre.joinToString(separator = ", ")
    }

    if (other.thumbnail_url != null) {
        thumbnail_url = other.thumbnail_url
    }

    status = other.status.toInt()

    if (!initialized) {
        initialized = other.initialized
    }
}
