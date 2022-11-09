package eu.kanade.domain.episode.model

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.TriStateFilter
import eu.kanade.domain.anime.model.isLocal
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadManager
import eu.kanade.tachiyomi.data.animedownload.model.AnimeDownload
import eu.kanade.tachiyomi.ui.anime.EpisodeItem
import eu.kanade.tachiyomi.util.episode.getEpisodeSort

/**
 * Applies the view filters to the list of episodes obtained from the database.
 * @return an observable of the list of episodes filtered and sorted.
 */
fun List<Episode>.applyFilters(anime: Anime, downloadManager: AnimeDownloadManager): List<Episode> {
    val isLocalAnime = anime.isLocal()
    val unseenFilter = anime.unseenFilter
    val downloadedFilter = anime.downloadedFilter
    val bookmarkedFilter = anime.bookmarkedFilter

    return filter { episode ->
        when (unseenFilter) {
            TriStateFilter.DISABLED -> true
            TriStateFilter.ENABLED_IS -> !episode.seen
            TriStateFilter.ENABLED_NOT -> episode.seen
        }
    }
        .filter { episode ->
            when (bookmarkedFilter) {
                TriStateFilter.DISABLED -> true
                TriStateFilter.ENABLED_IS -> episode.bookmark
                TriStateFilter.ENABLED_NOT -> !episode.bookmark
            }
        }
        .filter { episode ->
            val downloaded = downloadManager.isEpisodeDownloaded(episode.name, episode.scanlator, anime.title, anime.source)
            val downloadState = when {
                downloaded -> AnimeDownload.State.DOWNLOADED
                else -> AnimeDownload.State.NOT_DOWNLOADED
            }
            when (downloadedFilter) {
                TriStateFilter.DISABLED -> true
                TriStateFilter.ENABLED_IS -> downloadState == AnimeDownload.State.DOWNLOADED || isLocalAnime
                TriStateFilter.ENABLED_NOT -> downloadState != AnimeDownload.State.DOWNLOADED && !isLocalAnime
            }
        }
        .sortedWith(getEpisodeSort(anime))
}

/**
 * Applies the view filters to the list of episodes obtained from the database.
 * @return an observable of the list of episodes filtered and sorted.
 */
fun List<EpisodeItem>.applyFilters(anime: Anime): Sequence<EpisodeItem> {
    val isLocalAnime = anime.isLocal()
    val unreadFilter = anime.unseenFilter
    val downloadedFilter = anime.downloadedFilter
    val bookmarkedFilter = anime.bookmarkedFilter
    return asSequence()
        .filter { (episode) ->
            when (unreadFilter) {
                TriStateFilter.DISABLED -> true
                TriStateFilter.ENABLED_IS -> !episode.seen
                TriStateFilter.ENABLED_NOT -> episode.seen
            }
        }
        .filter { (episode) ->
            when (bookmarkedFilter) {
                TriStateFilter.DISABLED -> true
                TriStateFilter.ENABLED_IS -> episode.bookmark
                TriStateFilter.ENABLED_NOT -> !episode.bookmark
            }
        }
        .filter {
            when (downloadedFilter) {
                TriStateFilter.DISABLED -> true
                TriStateFilter.ENABLED_IS -> it.isDownloaded || isLocalAnime
                TriStateFilter.ENABLED_NOT -> !it.isDownloaded && !isLocalAnime
            }
        }
        .sortedWith { (episode1), (episode2) -> getEpisodeSort(anime).invoke(episode1, episode2) }
}
