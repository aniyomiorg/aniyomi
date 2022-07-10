package eu.kanade.tachiyomi.util.episode

import eu.kanade.domain.anime.interactor.GetFavorites
import eu.kanade.domain.anime.interactor.SetAnimeEpisodeFlags
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.lang.launchIO
import uy.kohesive.injekt.injectLazy

object EpisodeSettingsHelper {

    private val prefs: PreferencesHelper by injectLazy()
    private val getFavorites: GetFavorites by injectLazy()
    private val setAnimeEpisodeFlags: SetAnimeEpisodeFlags by injectLazy()

    /**
     * Updates the global Episode Settings in Preferences.
     */
    fun setGlobalSettings(anime: Anime) {
        prefs.setEpisodeSettingsDefault(anime.toDbAnime())
    }

    /**
     * Updates a single anime's Episode Settings to match what's set in Preferences.
     */
    fun applySettingDefaults(anime: Anime) {
        launchIO {
            setAnimeEpisodeFlags.awaitSetAllFlags(
                animeId = anime.id,
                unseenFilter = prefs.filterEpisodeBySeen().toLong(),
                downloadedFilter = prefs.filterEpisodeByDownloaded().toLong(),
                bookmarkedFilter = prefs.filterEpisodeByBookmarked().toLong(),
                sortingMode = prefs.sortEpisodeBySourceOrNumber().toLong(),
                sortingDirection = prefs.sortEpisodeByAscendingOrDescending().toLong(),
                displayMode = prefs.displayEpisodeByNameOrNumber().toLong(),
            )
        }
    }

    suspend fun applySettingDefaults(animeId: Long) {
        setAnimeEpisodeFlags.awaitSetAllFlags(
            animeId = animeId,
            unseenFilter = prefs.filterEpisodeBySeen().toLong(),
            downloadedFilter = prefs.filterEpisodeByDownloaded().toLong(),
            bookmarkedFilter = prefs.filterEpisodeByBookmarked().toLong(),
            sortingMode = prefs.sortEpisodeBySourceOrNumber().toLong(),
            sortingDirection = prefs.sortEpisodeByAscendingOrDescending().toLong(),
            displayMode = prefs.displayEpisodeByNameOrNumber().toLong(),
        )
    }

    /**
     * Updates all animes in library with global Episode Settings.
     */
    fun updateAllAnimesWithGlobalDefaults() {
        launchIO {
            getFavorites.await()
                .map { anime ->
                    setAnimeEpisodeFlags.awaitSetAllFlags(
                        animeId = anime.id,
                        unseenFilter = prefs.filterEpisodeBySeen().toLong(),
                        downloadedFilter = prefs.filterEpisodeByDownloaded().toLong(),
                        bookmarkedFilter = prefs.filterEpisodeByBookmarked().toLong(),
                        sortingMode = prefs.sortEpisodeBySourceOrNumber().toLong(),
                        sortingDirection = prefs.sortEpisodeByAscendingOrDescending().toLong(),
                        displayMode = prefs.displayEpisodeByNameOrNumber().toLong(),
                    )
                }
        }
    }
}
