package eu.kanade.tachiyomi.util.episode

import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadCache
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Returns a copy of the list with not downloaded chapters removed
 */
fun List<Episode>.filterDownloadedEpisodes(anime: Anime): List<Episode> {
    val downloadCache: AnimeDownloadCache = Injekt.get()

    return filter { downloadCache.isEpisodeDownloaded(it.name, it.scanlator, anime.title, anime.source, false) }
}
