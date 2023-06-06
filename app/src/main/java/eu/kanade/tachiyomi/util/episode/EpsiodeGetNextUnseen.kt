package eu.kanade.tachiyomi.util.episode

import eu.kanade.domain.items.episode.model.applyFilters
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.ui.entries.anime.EpisodeItem
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode

/**
 * Gets next unseen episode with filters and sorting applied
 */
fun List<Episode>.getNextUnseen(anime: Anime, downloadManager: AnimeDownloadManager): Episode? {
    return applyFilters(anime, downloadManager).let { episodes ->
        if (anime.sortDescending()) {
            episodes.findLast { !it.seen }
        } else {
            episodes.find { !it.seen }
        }
    }
}

/**
 * Gets next unseen episode with filters and sorting applied
 */
fun List<EpisodeItem>.getNextUnseen(anime: Anime): Episode? {
    return applyFilters(anime).let { episodes ->
        if (anime.sortDescending()) {
            episodes.findLast { !it.episode.seen }
        } else {
            episodes.find { !it.episode.seen }
        }
    }?.episode
}
