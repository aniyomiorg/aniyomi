package eu.kanade.tachiyomi.util.episode

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
    fun setGlobalSettings(anime: Anime?) {
        anime?.let {
            prefs.setEpisodeSettingsDefault(it)
            db.updateEpisodeFlags(it).executeAsBlocking()
        }
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

    /**
     * Updates all animes in library with global Episode Settings.
     */
    fun updateAllAnimesWithGlobalDefaults() {
        launchIO {
            val updatedAnimes = db.getAnimes().executeAsBlocking().map { anime ->
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
