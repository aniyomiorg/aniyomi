package eu.kanade.tachiyomi.util.episode

import eu.kanade.domain.anime.interactor.SetAnimeEpisodeFlags
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.lang.launchIO
import uy.kohesive.injekt.injectLazy

object EpisodeSettingsHelper {

    private val prefs: PreferencesHelper by injectLazy()
    private val db: AnimeDatabaseHelper by injectLazy()

    /**
     * Updates the global Episode Settings in Preferences.
     */
    fun setGlobalSettings(anime: Anime) {
        prefs.setEpisodeSettingsDefault(anime)
    }

    /**
     * Updates a single anime's Episode Settings to match what's set in Preferences.
     */
    fun applySettingDefaults(anime: Anime) {
        with(anime) {
            seenFilter = prefs.filterEpisodeBySeen()
            downloadedFilter = prefs.filterEpisodeByDownloaded()
            bookmarkedFilter = prefs.filterEpisodeByBookmarked()
            sorting = prefs.sortEpisodeBySourceOrNumber()
            displayMode = prefs.displayEpisodeByNameOrNumber()
            setEpisodeOrder(prefs.sortEpisodeByAscendingOrDescending())
        }

        db.updateEpisodeFlags(anime).executeAsBlocking()
    }

    suspend fun applySettingDefaults(animeId: Long, setAnimeEpisodeFlags: SetAnimeEpisodeFlags) {
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
            val updatedAnimes = db.getFavoriteAnimes()
                .executeAsBlocking()
                .map { anime ->
                    with(anime) {
                        seenFilter = prefs.filterEpisodeBySeen()
                        downloadedFilter = prefs.filterEpisodeByDownloaded()
                        bookmarkedFilter = prefs.filterEpisodeByBookmarked()
                        sorting = prefs.sortEpisodeBySourceOrNumber()
                        displayMode = prefs.displayEpisodeByNameOrNumber()
                        setEpisodeOrder(prefs.sortEpisodeByAscendingOrDescending())
                    }
                    anime
                }

            db.updateEpisodeFlags(updatedAnimes).executeAsBlocking()
        }
    }
}
