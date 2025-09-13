package tachiyomi.domain.items.season.interactor

import aniyomi.domain.anime.SeasonDisplayMode
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.entries.anime.interactor.GetAnimeFavorites
import tachiyomi.domain.entries.anime.interactor.SetAnimeSeasonFlags
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.service.LibraryPreferences

class SetAnimeDefaultSeasonFlags(
    private val libraryPreferences: LibraryPreferences,
    private val setAnimeSeasonFlags: SetAnimeSeasonFlags,
    private val getAnimeFavorites: GetAnimeFavorites,
) {
    suspend fun await(anime: Anime) {
        withNonCancellableContext {
            with(libraryPreferences) {
                setAnimeSeasonFlags.awaitSetAllFlags(
                    animeId = anime.id,
                    downloadFilter = filterSeasonByDownload().get(),
                    unseenFilter = filterSeasonByUnseen().get(),
                    startedFilter = filterSeasonByStarted().get(),
                    completedFilter = filterSeasonByCompleted().get(),
                    bookmarkedFilter = filterSeasonByBookmarked().get(),
                    fillermarkedFilter = filterSeasonByFillermarked().get(),
                    sortingMode = sortSeasonBySourceOrNumber().get(),
                    sortingDirection = sortSeasonByAscendingOrDescending().get(),
                    displayGridMode = SeasonDisplayMode.fromLong(seasonDisplayGridMode().get()),
                    displayGridSize = seasonDisplayGridSize().get(),
                    downloadedOverlay = seasonDownloadOverlay().get(),
                    unseenOverlay = seasonUnseenOverlay().get(),
                    localOverlay = seasonLocalOverlay().get(),
                    langOverlay = seasonLangOverlay().get(),
                    continueOverlay = seasonContinueOverlay().get(),
                    displayMode = seasonDisplayMode().get(),
                )
            }
        }
    }

    suspend fun awaitAll() {
        withNonCancellableContext {
            getAnimeFavorites.await().forEach { await(it) }
        }
    }
}
