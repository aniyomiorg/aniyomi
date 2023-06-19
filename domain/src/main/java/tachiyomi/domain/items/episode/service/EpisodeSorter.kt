package tachiyomi.domain.items.episode.service

import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode

fun getEpisodeSort(anime: Anime, sortDescending: Boolean = anime.sortDescending()): (Episode, Episode) -> Int {
    return when (anime.sorting) {
        Anime.EPISODE_SORTING_SOURCE -> when (sortDescending) {
            true -> { e1, e2 -> e1.sourceOrder.compareTo(e2.sourceOrder) }
            false -> { e1, e2 -> e2.sourceOrder.compareTo(e1.sourceOrder) }
        }
        Anime.EPISODE_SORTING_NUMBER -> when (sortDescending) {
            true -> { e1, e2 -> e2.episodeNumber.compareTo(e1.episodeNumber) }
            false -> { e1, e2 -> e1.episodeNumber.compareTo(e2.episodeNumber) }
        }
        Anime.EPISODE_SORTING_UPLOAD_DATE -> when (sortDescending) {
            true -> { e1, e2 -> e2.dateUpload.compareTo(e1.dateUpload) }
            false -> { e1, e2 -> e1.dateUpload.compareTo(e2.dateUpload) }
        }
        else -> throw NotImplementedError("Invalid episode sorting method: ${anime.sorting}")
    }
}
