package tachiyomi.domain.entries.anime.interactor

import aniyomi.domain.anime.SeasonDisplayMode
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUpdate
import tachiyomi.domain.entries.anime.repository.AnimeRepository

class SetAnimeSeasonFlags(
    private val animeRepository: AnimeRepository,
) {
    suspend fun awaitSetDownloadedFilter(anime: Anime, flag: Long): Boolean {
        return setFlag(anime, flag, Anime.SEASON_DOWNLOADED_MASK)
    }

    suspend fun awaitSetUnseenFilter(anime: Anime, flag: Long): Boolean {
        return setFlag(anime, flag, Anime.SEASON_UNSEEN_MASK)
    }

    suspend fun awaitSetStartedFilter(anime: Anime, flag: Long): Boolean {
        return setFlag(anime, flag, Anime.SEASON_STARTED_MASK)
    }

    suspend fun awaitSetCompletedFilter(anime: Anime, flag: Long): Boolean {
        return setFlag(anime, flag, Anime.SEASON_COMPLETED_MASK)
    }

    suspend fun awaitSetBookmarkedFilter(anime: Anime, flag: Long): Boolean {
        return setFlag(anime, flag, Anime.SEASON_BOOKMARKED_MASK)
    }

    suspend fun awaitSetFillermarkedFilter(anime: Anime, flag: Long): Boolean {
        return setFlag(anime, flag, Anime.SEASON_FILLERMARKED_MASK)
    }

    suspend fun awaitSetSortingModeOrFlipOrder(anime: Anime, flag: Long): Boolean {
        val newFlags = anime.seasonFlags.let {
            if (anime.seasonSorting == flag) {
                // Just flip the order
                val orderFlag = if (anime.seasonSortDescending()) {
                    Anime.SEASON_SORT_ASC
                } else {
                    Anime.SEASON_SORT_DESC
                }
                it.setFlag(orderFlag, Anime.SEASON_SORT_DIR_MASK)
            } else {
                // Set new flag with ascending order
                it
                    .setFlag(flag, Anime.SEASON_SORT_MASK)
                    .setFlag(Anime.SEASON_SORT_ASC, Anime.SEASON_SORT_DIR_MASK)
            }
        }
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                seasonFlags = newFlags,
            ),
        )
    }

    suspend fun awaitSetGridMode(anime: Anime, mode: SeasonDisplayMode): Boolean {
        val flag = SeasonDisplayMode.toLong(mode) shl Anime.SEASON_GRID_DISPLAY_MODE_BIT_OFFSET
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                seasonFlags = anime.seasonFlags.setFlag(flag, Anime.SEASON_GRID_DISPLAY_MODE_MASK),
            ),
        )
    }

    suspend fun awaitSetGridSize(anime: Anime, size: Int): Boolean {
        val flag = size.toLong() shl Anime.SEASON_GRID_DISPLAY_SIZE_BIT_OFFSET
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                seasonFlags = anime.seasonFlags.setFlag(flag, Anime.SEASON_GRID_DISPLAY_SIZE_MASK),
            ),
        )
    }

    suspend fun awaitSetDownloadedOverlay(anime: Anime, value: Boolean): Boolean {
        return setBooleanFlag(anime, value, Anime.SEASON_OVERLAY_DOWNLOADED_MASK)
    }

    suspend fun awaitSetUnseenOverlay(anime: Anime, value: Boolean): Boolean {
        return setBooleanFlag(anime, value, Anime.SEASON_OVERLAY_UNSEEN_MASK)
    }

    suspend fun awaitSetLocalOverlay(anime: Anime, value: Boolean): Boolean {
        return setBooleanFlag(anime, value, Anime.SEASON_OVERLAY_LOCAL_MASK)
    }

    suspend fun awaitSetLangOverlay(anime: Anime, value: Boolean): Boolean {
        return setBooleanFlag(anime, value, Anime.SEASON_OVERLAY_LANG_MASK)
    }

    suspend fun awaitSetContinueOverlay(anime: Anime, value: Boolean): Boolean {
        return setBooleanFlag(anime, value, Anime.SEASON_OVERLAY_CONT_MASK)
    }

    suspend fun awaitSetDisplayMode(anime: Anime, flag: Long): Boolean {
        return setFlag(anime, flag, Anime.SEASON_DISPLAY_MODE_MASK)
    }

    suspend fun awaitSetAllFlags(
        animeId: Long,
        downloadFilter: Long,
        unseenFilter: Long,
        startedFilter: Long,
        completedFilter: Long,
        bookmarkedFilter: Long,
        fillermarkedFilter: Long,
        sortingMode: Long,
        sortingDirection: Long,
        displayGridMode: SeasonDisplayMode,
        displayGridSize: Int,
        downloadedOverlay: Boolean,
        unseenOverlay: Boolean,
        localOverlay: Boolean,
        langOverlay: Boolean,
        continueOverlay: Boolean,
        displayMode: Long,
    ): Boolean {
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = animeId,
                seasonFlags = 0L.setFlag(downloadFilter, Anime.SEASON_DOWNLOADED_MASK)
                    .setFlag(unseenFilter, Anime.SEASON_UNSEEN_MASK)
                    .setFlag(startedFilter, Anime.SEASON_STARTED_MASK)
                    .setFlag(completedFilter, Anime.SEASON_COMPLETED_MASK)
                    .setFlag(bookmarkedFilter, Anime.SEASON_BOOKMARKED_MASK)
                    .setFlag(fillermarkedFilter, Anime.SEASON_FILLERMARKED_MASK)
                    .setFlag(sortingMode, Anime.SEASON_SORT_MASK)
                    .setFlag(sortingDirection, Anime.SEASON_SORT_DIR_MASK)
                    .setFlag(
                        SeasonDisplayMode.toLong(displayGridMode) shl Anime.SEASON_GRID_DISPLAY_MODE_BIT_OFFSET,
                        Anime.SEASON_GRID_DISPLAY_MODE_MASK,
                    )
                    .setFlag(
                        displayGridSize.toLong() shl Anime.SEASON_GRID_DISPLAY_SIZE_BIT_OFFSET,
                        Anime.SEASON_GRID_DISPLAY_SIZE_MASK,
                    )
                    .setFlag(
                        if (downloadedOverlay) Anime.SEASON_OVERLAY_DOWNLOADED_MASK else 0L,
                        Anime.SEASON_OVERLAY_DOWNLOADED_MASK,
                    )
                    .setFlag(
                        if (unseenOverlay) Anime.SEASON_OVERLAY_UNSEEN_MASK else 0L,
                        Anime.SEASON_OVERLAY_UNSEEN_MASK,
                    )
                    .setFlag(
                        if (localOverlay) Anime.SEASON_OVERLAY_LOCAL_MASK else 0L,
                        Anime.SEASON_OVERLAY_LOCAL_MASK,
                    )
                    .setFlag(
                        if (langOverlay) Anime.SEASON_OVERLAY_LANG_MASK else 0L,
                        Anime.SEASON_OVERLAY_LANG_MASK,
                    )
                    .setFlag(
                        if (continueOverlay) Anime.SEASON_OVERLAY_CONT_MASK else 0L,
                        Anime.SEASON_OVERLAY_CONT_MASK,
                    )
                    .setFlag(displayMode, Anime.SEASON_DISPLAY_MODE_MASK),
            ),
        )
    }

    private suspend fun setFlag(anime: Anime, flag: Long, mask: Long): Boolean {
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                seasonFlags = anime.seasonFlags.setFlag(flag, mask),
            ),
        )
    }

    private suspend fun setBooleanFlag(anime: Anime, value: Boolean, mask: Long): Boolean {
        val flag = if (value) mask else 0L
        return animeRepository.updateAnime(
            AnimeUpdate(
                id = anime.id,
                seasonFlags = anime.seasonFlags.setFlag(flag, mask),
            ),
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
