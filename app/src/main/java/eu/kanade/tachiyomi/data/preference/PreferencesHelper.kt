package eu.kanade.tachiyomi.data.preference

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.tfcporciuncula.flow.FlowSharedPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrationSourcesController
import eu.kanade.tachiyomi.ui.library.setting.DisplayModeSetting
import eu.kanade.tachiyomi.ui.library.setting.SortDirectionSetting
import eu.kanade.tachiyomi.ui.library.setting.SortModeSetting
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.preference.PreferenceValues as Values

class PreferencesHelper(val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val flowPrefs = FlowSharedPreferences(prefs)

    private val defaultDownloadsDir = File(
        Environment.getExternalStorageDirectory().absolutePath + File.separator +
            context.getString(R.string.app_name),
        "downloads"
    ).toUri()

    private val defaultBackupDir = File(
        Environment.getExternalStorageDirectory().absolutePath + File.separator +
            context.getString(R.string.app_name),
        "backup"
    ).toUri()

    fun startScreen() = prefs.getInt(Keys.startScreen, 1)

    fun confirmExit() = prefs.getBoolean(Keys.confirmExit, false)

    fun hideBottomBarOnScroll() = flowPrefs.getBoolean("pref_hide_bottom_bar_on_scroll", true)

    fun sideNavIconAlignment() = flowPrefs.getInt("pref_side_nav_icon_alignment", 0)

    fun useAuthenticator() = flowPrefs.getBoolean("use_biometric_lock", false)

    fun lockAppAfter() = flowPrefs.getInt("lock_app_after", 0)

    fun lastAppUnlock() = flowPrefs.getLong("last_app_unlock", 0)

    fun secureScreen() = flowPrefs.getBoolean("secure_screen", false)

    fun hideNotificationContent() = prefs.getBoolean(Keys.hideNotificationContent, false)

    fun autoUpdateMetadata() = prefs.getBoolean(Keys.autoUpdateMetadata, false)

    fun autoUpdateTrackers() = prefs.getBoolean(Keys.autoUpdateTrackers, false)

    fun themeMode() = flowPrefs.getEnum(
        "pref_theme_mode_key",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { Values.ThemeMode.system } else { Values.ThemeMode.light }
    )

    fun appTheme() = flowPrefs.getEnum(
        "pref_app_theme",
        if (DynamicColors.isDynamicColorAvailable()) { Values.AppTheme.MONET } else { Values.AppTheme.DEFAULT }
    )

    fun themeDarkAmoled() = flowPrefs.getBoolean("pref_theme_dark_amoled_key", false)

    fun doubleTapAnimSpeed() = flowPrefs.getInt("pref_double_tap_anim_speed", 500)

    fun trueColor() = flowPrefs.getBoolean("pref_true_color_key", false)

    fun fullscreen() = flowPrefs.getBoolean("fullscreen", true)

    fun grayscale() = flowPrefs.getBoolean("pref_grayscale", false)

    fun getPlayerSpeed() = prefs.getFloat(Keys.playerSpeed, 1F)

    fun setPlayerSpeed(newSpeed: Float) = prefs.edit {
        putFloat(Keys.playerSpeed, newSpeed)
    }

    fun getPlayerViewMode() = prefs.getInt(Keys.playerViewMode, 0)

    fun setPlayerViewMode(newMode: Int) = prefs.edit {
        putInt(Keys.playerViewMode, newMode)
    }

    fun pipPlayerPreference() = prefs.getBoolean(Keys.pipPlayerPreference, true)

    fun alwaysUseExternalPlayer() = prefs.getBoolean(Keys.alwaysUseExternalPlayer, false)

    fun externalPlayerPreference() = prefs.getString(Keys.externalPlayerPreference, "")

    fun progressPreference() = prefs.getString(Keys.progressPreference, "0.85F")!!.toFloat()

    fun skipLengthPreference() = prefs.getString(Keys.skipLengthPreference, "10")!!.toInt()

    fun readerTheme() = flowPrefs.getInt("pref_reader_theme_key", 1)

    fun cropBorders() = flowPrefs.getBoolean("crop_borders", false)

    fun portraitColumns() = flowPrefs.getInt("pref_library_columns_portrait_key", 0)

    fun landscapeColumns() = flowPrefs.getInt("pref_library_columns_landscape_key", 0)

    fun jumpToChapters() = prefs.getBoolean(Keys.jumpToChapters, false)

    fun autoUpdateTrack() = prefs.getBoolean(Keys.autoUpdateTrack, true)

    fun lastUsedAnimeSource() = flowPrefs.getLong("last_anime_catalogue_source", -1)

    fun lastUsedAnimeCategory() = flowPrefs.getInt("last_used_anime_category", 0)

    fun lastVersionCode() = flowPrefs.getInt("last_version_code", 0)

    fun sourceDisplayMode() = flowPrefs.getEnum("pref_display_mode_catalogue", DisplayModeSetting.COMPACT_GRID)

    fun enabledLanguages() = flowPrefs.getStringSet("source_languages", setOf("all", "en", Locale.getDefault().language))

    fun trackUsername(sync: TrackService) = prefs.getString(Keys.trackUsername(sync.id), "")

    fun trackPassword(sync: TrackService) = prefs.getString(Keys.trackPassword(sync.id), "")

    fun setTrackCredentials(sync: TrackService, username: String, password: String) {
        prefs.edit {
            putString(Keys.trackUsername(sync.id), username)
            putString(Keys.trackPassword(sync.id), password)
        }
    }

    fun trackToken(sync: TrackService) = flowPrefs.getString(Keys.trackToken(sync.id), "")

    fun anilistScoreType() = flowPrefs.getString("anilist_score_type", Anilist.POINT_10)

    fun backupsDirectory() = flowPrefs.getString("backup_directory", defaultBackupDir.toString())

    fun relativeTime() = flowPrefs.getInt("relative_time", 7)

    fun dateFormat(format: String = flowPrefs.getString(Keys.dateFormat, "").get()): DateFormat = when (format) {
        "" -> DateFormat.getDateInstance(DateFormat.SHORT)
        else -> SimpleDateFormat(format, Locale.getDefault())
    }

    fun downloadsDirectory() = flowPrefs.getString("download_directory", defaultDownloadsDir.toString())

    fun useExternalDownloader() = prefs.getBoolean(Keys.useExternalDownloader, false)

    fun externalDownloaderSelection() = prefs.getString(Keys.externalDownloaderSelection, "")

    fun downloadOnlyOverWifi() = prefs.getBoolean(Keys.downloadOnlyOverWifi, true)

    fun saveChaptersAsCBZ() = flowPrefs.getBoolean("save_chapter_as_cbz", false)

    fun numberOfBackups() = flowPrefs.getInt("backup_slots", 1)

    fun backupInterval() = flowPrefs.getInt("backup_interval", 0)

    fun removeAfterReadSlots() = prefs.getInt(Keys.removeAfterReadSlots, -1)

    fun removeAfterMarkedAsRead() = prefs.getBoolean(Keys.removeAfterMarkedAsRead, false)

    fun removeBookmarkedChapters() = prefs.getBoolean(Keys.removeBookmarkedChapters, false)

    // fun removeExcludeCategories() = flowPrefs.getStringSet("remove_exclude_categories", emptySet())
    fun removeExcludeAnimeCategories() = flowPrefs.getStringSet("remove_exclude_categories_anime", emptySet())

    fun libraryUpdateInterval() = flowPrefs.getInt("pref_library_update_interval_key", 24)

    fun libraryUpdateDeviceRestriction() = flowPrefs.getStringSet("library_update_restriction", setOf(DEVICE_ONLY_ON_WIFI))
    fun libraryUpdateMangaRestriction() = flowPrefs.getStringSet("library_update_manga_restriction", setOf(MANGA_FULLY_READ, MANGA_ONGOING))

    fun showUpdatesNavBadge() = flowPrefs.getBoolean("library_update_show_tab_badge", false)
    fun unreadUpdatesCount() = flowPrefs.getInt("library_unread_updates_count", 0)
    fun unseenUpdatesCount() = flowPrefs.getInt("library_unseen_updates_count", 0)

    fun animelibUpdateCategories() = flowPrefs.getStringSet("animelib_update_categories", emptySet())

    fun animelibUpdateCategoriesExclude() = flowPrefs.getStringSet("animelib_update_categories_exclude", emptySet())

    fun libraryDisplayMode() = flowPrefs.getEnum("pref_display_mode_library", DisplayModeSetting.COMPACT_GRID)

    fun downloadBadge() = flowPrefs.getBoolean("display_download_badge", false)

    fun localBadge() = flowPrefs.getBoolean("display_local_badge", true)

    fun downloadedOnly() = flowPrefs.getBoolean("pref_downloaded_only", false)

    fun unreadBadge() = flowPrefs.getBoolean("display_unread_badge", true)

    fun languageBadge() = flowPrefs.getBoolean("display_language_badge", false)

    fun animeCategoryTabs() = flowPrefs.getBoolean("display_anime_category_tabs", true)

    // fun categoryNumberOfItems() = flowPrefs.getBoolean("display_number_of_items", false)

    fun animeCategoryNumberOfItems() = flowPrefs.getBoolean("display_number_of_items_anime", false)

    fun filterDownloaded() = flowPrefs.getInt(Keys.filterDownloaded, ExtendedNavigationView.Item.TriStateGroup.State.IGNORE.value)

    fun filterUnread() = flowPrefs.getInt(Keys.filterUnread, ExtendedNavigationView.Item.TriStateGroup.State.IGNORE.value)

    fun filterCompleted() = flowPrefs.getInt(Keys.filterCompleted, ExtendedNavigationView.Item.TriStateGroup.State.IGNORE.value)

    fun filterTracking(name: Int) = flowPrefs.getInt("${Keys.filterTracked}_$name", ExtendedNavigationView.Item.TriStateGroup.State.IGNORE.value)

    fun librarySortingMode() = flowPrefs.getEnum(Keys.librarySortingMode, SortModeSetting.ALPHABETICAL)
    fun librarySortingAscending() = flowPrefs.getEnum(Keys.librarySortingDirection, SortDirectionSetting.ASCENDING)

    fun migrationSortingMode() = flowPrefs.getEnum(Keys.migrationSortingMode, MigrationSourcesController.SortSetting.ALPHABETICAL)
    fun migrationSortingDirection() = flowPrefs.getEnum(Keys.migrationSortingDirection, MigrationSourcesController.DirectionSetting.ASCENDING)

    fun automaticExtUpdates() = flowPrefs.getBoolean("automatic_ext_updates", true)

    fun showNsfwSource() = flowPrefs.getBoolean("show_nsfw_source", true)

    fun extensionUpdatesCount() = flowPrefs.getInt("ext_updates_count", 0)

    fun animeextensionUpdatesCount() = flowPrefs.getInt("animeext_updates_count", 0)

    fun lastAppCheck() = flowPrefs.getLong("last_app_check", 0)

    fun lastAnimeExtCheck() = flowPrefs.getLong("last_anime_ext_check", 0)

    fun searchPinnedSourcesOnly() = prefs.getBoolean(Keys.searchPinnedSourcesOnly, false)

    fun disabledAnimeSources() = flowPrefs.getStringSet("hidden_catalogues", emptySet())

    fun pinnedAnimeSources() = flowPrefs.getStringSet("pinned_anime_catalogues", emptySet())

    fun downloadNew() = flowPrefs.getBoolean("download_new", false)

    fun downloadNewCategoriesAnime() = flowPrefs.getStringSet("download_new_categories_anime", emptySet())

    fun downloadNewCategoriesAnimeExclude() = flowPrefs.getStringSet("download_new_categories_exclude_anime", emptySet())

    fun lang() = flowPrefs.getString("app_language", "")

    fun defaultAnimeCategory() = prefs.getInt(Keys.defaultAnimeCategory, -1)

    fun categorizedDisplaySettings() = flowPrefs.getBoolean("categorized_display", false)

    fun skipRead() = prefs.getBoolean(Keys.skipRead, false)

    fun skipFiltered() = prefs.getBoolean(Keys.skipFiltered, true)

    fun migrateFlags() = flowPrefs.getInt("migrate_flags", Int.MAX_VALUE)

    fun trustedSignatures() = flowPrefs.getStringSet("trusted_signatures", emptySet())

    fun dohProvider() = prefs.getInt(Keys.dohProvider, -1)

    fun lastSearchQuerySearchSettings() = flowPrefs.getString("last_search_query", "")

    fun filterEpisodeBySeen() = prefs.getInt(Keys.defaultEpisodeFilterByRead, Anime.SHOW_ALL)

    fun filterEpisodeByDownloaded() = prefs.getInt(Keys.defaultEpisodeFilterByDownloaded, Anime.SHOW_ALL)

    fun filterEpisodeByBookmarked() = prefs.getInt(Keys.defaultEpisodeFilterByBookmarked, Anime.SHOW_ALL)

    fun sortEpisodeBySourceOrNumber() = prefs.getInt(Keys.defaultEpisodeSortBySourceOrNumber, Anime.EPISODE_SORTING_SOURCE)

    fun displayEpisodeByNameOrNumber() = prefs.getInt(Keys.defaultEpisodeDisplayByNameOrNumber, Anime.EPISODE_DISPLAY_NAME)

    fun sortEpisodeByAscendingOrDescending() = prefs.getInt(Keys.defaultEpisodeSortByAscendingOrDescending, Anime.EPISODE_SORT_DESC)

    fun incognitoMode() = flowPrefs.getBoolean("incognito_mode", false)

    fun tabletUiMode() = flowPrefs.getEnum("tablet_ui_mode", Values.TabletUiMode.AUTOMATIC)

    fun extensionInstaller() = flowPrefs.getEnum(
        "extension_installer",
        if (DeviceUtil.isMiui) Values.ExtensionInstaller.LEGACY else Values.ExtensionInstaller.PACKAGEINSTALLER
    )

    fun verboseLogging() = prefs.getBoolean(Keys.verboseLogging, false)

    fun autoClearChapterCache() = prefs.getBoolean(Keys.autoClearChapterCache, false)

    fun setEpisodeSettingsDefault(anime: Anime) {
        prefs.edit {
            putInt(Keys.defaultEpisodeFilterByRead, anime.seenFilter)
            putInt(Keys.defaultEpisodeFilterByDownloaded, anime.downloadedFilter)
            putInt(Keys.defaultEpisodeFilterByBookmarked, anime.bookmarkedFilter)
            putInt(Keys.defaultEpisodeSortBySourceOrNumber, anime.sorting)
            putInt(Keys.defaultEpisodeDisplayByNameOrNumber, anime.displayMode)
            putInt(
                Keys.defaultEpisodeSortByAscendingOrDescending,
                if (anime.sortDescending()) Anime.EPISODE_SORT_DESC else Anime.EPISODE_SORT_ASC
            )
        }
    }
}
