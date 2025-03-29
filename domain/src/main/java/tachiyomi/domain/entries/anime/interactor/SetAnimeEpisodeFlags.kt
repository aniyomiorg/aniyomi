package tachiyomi.domain.entries.anime.interactor

import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUpdate
import tachiyomi.domain.entries.anime.repository.AnimeRepository

class SetAnimeEpisodeFlags(
    private val animeRepository: AnimeRepository,
) {

    suspend fun awaitSetDownloadedFilter(anime: Anime, flag: Long): Boolean {
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                episodeFlags = anime.episodeFlags.setFlag(flag, Anime.EPISODE_DOWNLOADED_MASK),
            ),
        )
    }

    suspend fun awaitSetUnseenFilter(anime: Anime, flag: Long): Boolean {
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                episodeFlags = anime.episodeFlags.setFlag(flag, Anime.EPISODE_UNSEEN_MASK),
            ),
        )
    }

    suspend fun awaitSetBookmarkFilter(anime: Anime, flag: Long): Boolean {
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                episodeFlags = anime.episodeFlags.setFlag(flag, Anime.EPISODE_BOOKMARKED_MASK),
            ),
        )
    }

    suspend fun awaitSetFillermarkFilter(anime: Anime, flag: Long): Boolean {
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                episodeFlags = anime.episodeFlags.setFlag(flag, Anime.EPISODE_FILLERMARKED_MASK),
            ),
        )
    }

    suspend fun awaitSetDisplayMode(anime: Anime, flag: Long): Boolean {
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                episodeFlags = anime.episodeFlags.setFlag(flag, Anime.EPISODE_DISPLAY_MASK),
            ),
        )
    }

    suspend fun awaitSetSortingModeOrFlipOrder(anime: Anime, flag: Long): Boolean {
        val newFlags = anime.episodeFlags.let {
            if (anime.sorting == flag) {
                // Just flip the order
                val orderFlag = if (anime.sortDescending()) {
                    Anime.EPISODE_SORT_ASC
                } else {
                    Anime.EPISODE_SORT_DESC
                }
                it.setFlag(orderFlag, Anime.EPISODE_SORT_DIR_MASK)
            } else {
                // Set new flag with ascending order
                it
                    .setFlag(flag, Anime.EPISODE_SORTING_MASK)
                    .setFlag(Anime.EPISODE_SORT_ASC, Anime.EPISODE_SORT_DIR_MASK)
            }
        }
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                episodeFlags = newFlags,
            ),
        )
    }

    suspend fun awaitShowEpisodePreviews(anime: Anime, flag: Long): Boolean {
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                episodeFlags = anime.episodeFlags.setFlag(flag, Anime.EPISODE_PREVIEWS_MASK),
            ),
        )
    }

    suspend fun awaitShowEpisodeSummaries(anime: Anime, flag: Long): Boolean {
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                episodeFlags = anime.episodeFlags.setFlag(flag, Anime.EPISODE_SUMMARIES_MASK),
            ),
        )
    }

    suspend fun awaitSetAllFlags(
        animeId: Long,
        unseenFilter: Long,
        downloadedFilter: Long,
        bookmarkedFilter: Long,
        fillermarkedFilter: Long,
        sortingMode: Long,
        sortingDirection: Long,
        displayMode: Long,
        showPreviews: Long,
        showSummaries: Long,
    ): Boolean {
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = animeId,
                episodeFlags = 0L.setFlag(unseenFilter, Anime.EPISODE_UNSEEN_MASK)
                    .setFlag(downloadedFilter, Anime.EPISODE_DOWNLOADED_MASK)
                    .setFlag(bookmarkedFilter, Anime.EPISODE_BOOKMARKED_MASK)
                    .setFlag(fillermarkedFilter, Anime.EPISODE_FILLERMARKED_MASK)
                    .setFlag(sortingMode, Anime.EPISODE_SORTING_MASK)
                    .setFlag(sortingDirection, Anime.EPISODE_SORT_DIR_MASK)
                    .setFlag(displayMode, Anime.EPISODE_DISPLAY_MASK)
                    .setFlag(showPreviews, Anime.EPISODE_PREVIEWS_MASK)
                    .setFlag(showSummaries, Anime.EPISODE_SUMMARIES_MASK),
            ),
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
