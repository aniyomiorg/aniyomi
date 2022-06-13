package eu.kanade.tachiyomi.util.episode

import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode

fun getEpisodeSort(anime: Anime, sortDescending: Boolean = anime.sortDescending()): (Episode, Episode) -> Int {
    return when (anime.sorting) {
        Anime.EPISODE_SORTING_SOURCE -> when (sortDescending) {
            true -> { e1, e2 -> e1.source_order.compareTo(e2.source_order) }
            false -> { e1, e2 -> e2.source_order.compareTo(e1.source_order) }
        }
        Anime.EPISODE_SORTING_NUMBER -> when (sortDescending) {
            true -> { e1, e2 -> e2.episode_number.compareTo(e1.episode_number) }
            false -> { e1, e2 -> e1.episode_number.compareTo(e2.episode_number) }
        }
        Anime.EPISODE_SORTING_UPLOAD_DATE -> when (sortDescending) {
            true -> { e1, e2 -> e2.date_upload.compareTo(e1.date_upload) }
            false -> { e1, e2 -> e1.date_upload.compareTo(e2.date_upload) }
        }
        else -> throw NotImplementedError("Invalid episode sorting method: ${anime.sorting}")
    }
}
