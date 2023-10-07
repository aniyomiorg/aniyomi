package eu.kanade.tachiyomi

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.backup.BackupCreateJob
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.util.preference.minusAssign
import eu.kanade.tachiyomi.util.preference.plusAssign
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.workManager
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.preference.getEnum
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.entries.TriStateFilter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_COMPLETED
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

object Migrations {

    /**
     * Performs a migration when the application is updated.
     *
     * @return true if a migration is performed, false otherwise.
     */
    fun upgrade(
        context: Context,
        preferenceStore: PreferenceStore,
        basePreferences: BasePreferences,
        uiPreferences: UiPreferences,
        networkPreferences: NetworkPreferences,
        sourcePreferences: SourcePreferences,
        securityPreferences: SecurityPreferences,
        libraryPreferences: LibraryPreferences,
        readerPreferences: ReaderPreferences,
        playerPreferences: PlayerPreferences,
        backupPreferences: BackupPreferences,
        trackManager: TrackManager,
    ): Boolean {
        val lastVersionCode = preferenceStore.getInt("last_version_code", 0)
        val oldVersion = lastVersionCode.get()
        if (oldVersion < BuildConfig.VERSION_CODE) {
            lastVersionCode.set(BuildConfig.VERSION_CODE)

            // Always set up background tasks to ensure they're running
            MangaLibraryUpdateJob.setupTask(context)
            AnimeLibraryUpdateJob.setupTask(context)
            BackupCreateJob.setupTask(context)

            // Fresh install
            if (oldVersion == 0) {
                return false
            }

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)

            if (oldVersion < 14) {
                // Restore jobs after upgrading to Evernote's job scheduler.
                MangaLibraryUpdateJob.setupTask(context)
                AnimeLibraryUpdateJob.setupTask(context)
            }
            if (oldVersion < 15) {
                // Delete internal chapter cache dir.
                File(context.cacheDir, "chapter_disk_cache").deleteRecursively()
            }
            if (oldVersion < 19) {
                // Move covers to external files dir.
                val oldDir = File(context.externalCacheDir, "cover_disk_cache")
                if (oldDir.exists()) {
                    val destDir = context.getExternalFilesDir("covers")
                    if (destDir != null) {
                        oldDir.listFiles()?.forEach {
                            it.renameTo(File(destDir, it.name))
                        }
                    }
                }
            }
            if (oldVersion < 26) {
                // Delete external chapter cache dir.
                val extCache = context.externalCacheDir
                if (extCache != null) {
                    val chapterCache = File(extCache, "chapter_disk_cache")
                    if (chapterCache.exists()) {
                        chapterCache.deleteRecursively()
                    }
                }
            }
            if (oldVersion < 43) {
                // Restore jobs after migrating from Evernote's job scheduler to WorkManager.
                MangaLibraryUpdateJob.setupTask(context)
                AnimeLibraryUpdateJob.setupTask(context)
                BackupCreateJob.setupTask(context)
            }
            if (oldVersion < 44) {
                // Reset sorting preference if using removed sort by source
                val oldMangaSortingMode = prefs.getInt(libraryPreferences.libraryMangaSortingMode().key(), 0)

                if (oldMangaSortingMode == 5) { // SOURCE = 5
                    prefs.edit {
                        putInt(libraryPreferences.libraryMangaSortingMode().key(), 0) // ALPHABETICAL = 0
                    }
                }

                val oldAnimeSortingMode = prefs.getInt(libraryPreferences.libraryAnimeSortingMode().key(), 0)

                if (oldAnimeSortingMode == 5) { // SOURCE = 5
                    prefs.edit {
                        putInt(libraryPreferences.libraryAnimeSortingMode().key(), 0) // ALPHABETICAL = 0
                    }
                }
            }
            if (oldVersion < 52) {
                // Migrate library filters to tri-state versions
                fun convertBooleanPrefToTriState(key: String): Int {
                    val oldPrefValue = prefs.getBoolean(key, false)
                    return if (oldPrefValue) {
                        1
                    } else {
                        0
                    }
                }
                prefs.edit {
                    putInt(libraryPreferences.filterDownloadedManga().key(), convertBooleanPrefToTriState("pref_filter_downloaded_key"))
                    remove("pref_filter_downloaded_key")

                    putInt(libraryPreferences.filterUnread().key(), convertBooleanPrefToTriState("pref_filter_unread_key"))
                    remove("pref_filter_unread_key")

                    putInt(libraryPreferences.filterCompletedManga().key(), convertBooleanPrefToTriState("pref_filter_completed_key"))
                    remove("pref_filter_completed_key")
                }
            }
            if (oldVersion < 54) {
                // Force MAL log out due to login flow change
                // v52: switched from scraping to WebView
                // v53: switched from WebView to OAuth
                val trackManager = Injekt.get<TrackManager>()
                if (trackManager.myAnimeList.isLogged) {
                    trackManager.myAnimeList.logout()
                    context.toast(R.string.myanimelist_relogin)
                }
            }
            if (oldVersion < 57) {
                // Migrate DNS over HTTPS setting
                val wasDohEnabled = prefs.getBoolean("enable_doh", false)
                if (wasDohEnabled) {
                    prefs.edit {
                        putInt(networkPreferences.dohProvider().key(), PREF_DOH_CLOUDFLARE)
                        remove("enable_doh")
                    }
                }
            }
            if (oldVersion < 59) {
                // Reset rotation to Free after replacing Lock
                if (prefs.contains("pref_rotation_type_key")) {
                    prefs.edit {
                        putInt("pref_rotation_type_key", 1)
                    }
                }
            }
            if (oldVersion < 60) {
                // Migrate Rotation and Viewer values to default values for viewer_flags
                val newOrientation = when (prefs.getInt("pref_rotation_type_key", 1)) {
                    1 -> OrientationType.FREE.flagValue
                    2 -> OrientationType.PORTRAIT.flagValue
                    3 -> OrientationType.LANDSCAPE.flagValue
                    4 -> OrientationType.LOCKED_PORTRAIT.flagValue
                    5 -> OrientationType.LOCKED_LANDSCAPE.flagValue
                    else -> OrientationType.FREE.flagValue
                }

                // Reading mode flag and prefValue is the same value
                val newReadingMode = prefs.getInt("pref_default_viewer_key", 1)

                prefs.edit {
                    putInt("pref_default_orientation_type_key", newOrientation)
                    remove("pref_rotation_type_key")
                    putInt("pref_default_reading_mode_key", newReadingMode)
                    remove("pref_default_viewer_key")
                }
            }
            if (oldVersion < 61) {
                // Handle removed every 1 or 2 hour library updates
                val updateInterval = libraryPreferences.libraryUpdateInterval().get()
                if (updateInterval == 1 || updateInterval == 2) {
                    libraryPreferences.libraryUpdateInterval().set(3)
                    MangaLibraryUpdateJob.setupTask(context, 3)
                    AnimeLibraryUpdateJob.setupTask(context, 3)
                }
            }
            if (oldVersion < 64) {
                // Set up background tasks
                MangaLibraryUpdateJob.setupTask(context)
                AnimeLibraryUpdateJob.setupTask(context)
            }
            if (oldVersion < 64) {
                val oldMangaSortingMode = prefs.getInt(libraryPreferences.libraryMangaSortingMode().key(), 0)
                val oldAnimeSortingMode = prefs.getInt(libraryPreferences.libraryAnimeSortingMode().key(), 0)
                val oldSortingDirection = prefs.getBoolean("library_sorting_ascending", true)

                val newMangaSortingMode = when (oldMangaSortingMode) {
                    0 -> "ALPHABETICAL"
                    1 -> "LAST_READ"
                    2 -> "LAST_CHECKED"
                    3 -> "UNREAD"
                    4 -> "TOTAL_CHAPTERS"
                    6 -> "LATEST_CHAPTER"
                    8 -> "DATE_FETCHED"
                    7 -> "DATE_ADDED"
                    else -> "ALPHABETICAL"
                }

                val newAnimeSortingMode = when (oldAnimeSortingMode) {
                    0 -> "ALPHABETICAL"
                    1 -> "LAST_SEEN"
                    2 -> "LAST_CHECKED"
                    3 -> "UNSEEN"
                    4 -> "TOTAL_EPISODES"
                    6 -> "LATEST_EPISODE"
                    8 -> "DATE_FETCHED"
                    7 -> "DATE_ADDED"
                    else -> "ALPHABETICAL"
                }

                val newSortingDirection = when (oldSortingDirection) {
                    true -> "ASCENDING"
                    else -> "DESCENDING"
                }

                prefs.edit(commit = true) {
                    remove(libraryPreferences.libraryMangaSortingMode().key())
                    remove(libraryPreferences.libraryAnimeSortingMode().key())
                    remove("library_sorting_ascending")
                }

                prefs.edit {
                    putString(libraryPreferences.libraryMangaSortingMode().key(), newMangaSortingMode)
                    putString(libraryPreferences.libraryAnimeSortingMode().key(), newAnimeSortingMode)
                    putString("library_sorting_ascending", newSortingDirection)
                }
            }
            if (oldVersion < 70) {
                if (sourcePreferences.enabledLanguages().isSet()) {
                    sourcePreferences.enabledLanguages() += "all"
                }
            }
            if (oldVersion < 71) {
                // Handle removed every 3, 4, 6, and 8 hour library updates
                val updateInterval = libraryPreferences.libraryUpdateInterval().get()
                if (updateInterval in listOf(3, 4, 6, 8)) {
                    libraryPreferences.libraryUpdateInterval().set(12)
                    MangaLibraryUpdateJob.setupTask(context, 12)
                    AnimeLibraryUpdateJob.setupTask(context, 12)
                }
            }
            if (oldVersion < 72) {
                val oldUpdateOngoingOnly = prefs.getBoolean("pref_update_only_non_completed_key", true)
                if (!oldUpdateOngoingOnly) {
                    libraryPreferences.libraryUpdateItemRestriction() -= ENTRY_NON_COMPLETED
                }
            }
            if (oldVersion < 75) {
                val oldSecureScreen = prefs.getBoolean("secure_screen", false)
                if (oldSecureScreen) {
                    securityPreferences.secureScreen().set(SecurityPreferences.SecureScreenMode.ALWAYS)
                }
                if (DeviceUtil.isMiui && basePreferences.extensionInstaller().get() == BasePreferences.ExtensionInstaller.PACKAGEINSTALLER) {
                    basePreferences.extensionInstaller().set(BasePreferences.ExtensionInstaller.LEGACY)
                }
            }
            if (oldVersion < 76) {
                BackupCreateJob.setupTask(context)
            }
            if (oldVersion < 77) {
                val oldReaderTap = prefs.getBoolean("reader_tap", false)
                if (!oldReaderTap) {
                    readerPreferences.navigationModePager().set(5)
                    readerPreferences.navigationModeWebtoon().set(5)
                }
            }
            if (oldVersion < 81) {
                // Handle renamed enum values
                prefs.edit {
                    val newMangaSortingMode = when (val oldSortingMode = prefs.getString(libraryPreferences.libraryMangaSortingMode().key(), "ALPHABETICAL")) {
                        "LAST_CHECKED" -> "LAST_MANGA_UPDATE"
                        "UNREAD" -> "UNREAD_COUNT"
                        "DATE_FETCHED" -> "CHAPTER_FETCH_DATE"
                        else -> oldSortingMode
                    }
                    val newAnimeSortingMode = when (val oldSortingMode = prefs.getString(libraryPreferences.libraryAnimeSortingMode().key(), "ALPHABETICAL")) {
                        "LAST_CHECKED" -> "LAST_MANGA_UPDATE"
                        "UNREAD" -> "UNREAD_COUNT"
                        "DATE_FETCHED" -> "CHAPTER_FETCH_DATE"
                        else -> oldSortingMode
                    }
                    putString(libraryPreferences.libraryMangaSortingMode().key(), newMangaSortingMode)
                    putString(libraryPreferences.libraryAnimeSortingMode().key(), newAnimeSortingMode)
                }
            }
            if (oldVersion < 82) {
                prefs.edit {
                    val mangasort = prefs.getString(libraryPreferences.libraryMangaSortingMode().key(), null) ?: return@edit
                    val animesort = prefs.getString(libraryPreferences.libraryAnimeSortingMode().key(), null) ?: return@edit
                    val direction = prefs.getString("library_sorting_ascending", "ASCENDING")!!
                    putString(libraryPreferences.libraryMangaSortingMode().key(), "$mangasort,$direction")
                    putString(libraryPreferences.libraryAnimeSortingMode().key(), "$animesort,$direction")
                    remove("library_sorting_ascending")
                }
            }
            if (oldVersion < 84) {
                if (backupPreferences.numberOfBackups().get() == 1) {
                    backupPreferences.numberOfBackups().set(2)
                }
                if (backupPreferences.backupInterval().get() == 0) {
                    backupPreferences.backupInterval().set(12)
                    BackupCreateJob.setupTask(context)
                }
            }
            if (oldVersion < 85) {
                val preferences = listOf(
                    libraryPreferences.filterChapterByRead(),
                    libraryPreferences.filterChapterByDownloaded(),
                    libraryPreferences.filterChapterByBookmarked(),
                    libraryPreferences.sortChapterBySourceOrNumber(),
                    libraryPreferences.displayChapterByNameOrNumber(),
                    libraryPreferences.sortChapterByAscendingOrDescending(),
                    libraryPreferences.filterEpisodeBySeen(),
                    libraryPreferences.filterEpisodeByDownloaded(),
                    libraryPreferences.filterEpisodeByBookmarked(),
                    libraryPreferences.sortEpisodeBySourceOrNumber(),
                    libraryPreferences.displayEpisodeByNameOrNumber(),
                    libraryPreferences.sortEpisodeByAscendingOrDescending(),
                )

                prefs.edit {
                    preferences.forEach { preference ->
                        val key = preference.key()
                        val value = prefs.getInt(key, Int.MIN_VALUE)
                        if (value == Int.MIN_VALUE) return@forEach
                        remove(key)
                        putLong(key, value.toLong())
                    }
                }
            }
            if (oldVersion < 86) {
                if (uiPreferences.themeMode().isSet()) {
                    prefs.edit {
                        val themeMode = prefs.getString(uiPreferences.themeMode().key(), null) ?: return@edit
                        putString(uiPreferences.themeMode().key(), themeMode.uppercase())
                    }
                }
            }
            if (oldVersion < 92) {
                if (playerPreferences.progressPreference().isSet()) {
                    prefs.edit {
                        val progressString = try {
                            prefs.getString(playerPreferences.progressPreference().key(), null)
                        } catch (e: ClassCastException) {
                            null
                        } ?: return@edit
                        val newProgress = progressString.toFloatOrNull() ?: return@edit
                        putFloat(playerPreferences.progressPreference().key(), newProgress)
                    }
                }
            }
            if (oldVersion < 93) {
                listOf(
                    playerPreferences.defaultPlayerOrientationType(),
                    playerPreferences.defaultPlayerOrientationLandscape(),
                    playerPreferences.defaultPlayerOrientationPortrait(),
                    playerPreferences.skipLengthPreference(),
                ).forEach { pref ->
                    if (pref.isSet()) {
                        prefs.edit {
                            val oldString = try {
                                prefs.getString(pref.key(), null)
                            } catch (e: ClassCastException) {
                                null
                            } ?: return@edit
                            val newInt = oldString.toIntOrNull() ?: return@edit
                            putInt(pref.key(), newInt)
                        }
                        val trackingQueuePref =
                            context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)
                        trackingQueuePref.all.forEach {
                            val (_, lastChapterRead) = it.value.toString().split(":")
                            trackingQueuePref.edit {
                                remove(it.key)
                                putFloat(it.key, lastChapterRead.toFloat())
                            }
                        }
                    }
                    if (oldVersion < 96) {
                        MangaLibraryUpdateJob.cancelAllWorks(context)
                        AnimeLibraryUpdateJob.cancelAllWorks(context)
                        MangaLibraryUpdateJob.setupTask(context)
                        AnimeLibraryUpdateJob.setupTask(context)
                    }
                    if (oldVersion < 97) {
                        // Removed background jobs
                        context.workManager.cancelAllWorkByTag("UpdateChecker")
                        context.workManager.cancelAllWorkByTag("ExtensionUpdate")
                        prefs.edit {
                            remove("automatic_ext_updates")
                        }
                    }
                    if (oldVersion < 99) {
                        val prefKeys = listOf(
                            "pref_filter_library_downloaded",
                            "pref_filter_library_unread",
                            "pref_filter_library_unseen",
                            "pref_filter_library_started",
                            "pref_filter_library_bookmarked",
                            "pref_filter_library_completed",
                        ) + trackManager.services.map { "pref_filter_library_tracked_${it.id}" }

                        prefKeys.forEach { key ->
                            val pref = preferenceStore.getInt(key, 0)
                            prefs.edit {
                                remove(key)

                                val newValue = when (pref.get()) {
                                    1 -> TriStateFilter.ENABLED_IS
                                    2 -> TriStateFilter.ENABLED_NOT
                                    else -> TriStateFilter.DISABLED
                                }

                                preferenceStore.getEnum("${key}_v2", TriStateFilter.DISABLED).set(newValue)
                            }
                        }
                    }
                    if (oldVersion < 100) {
                        BackupCreateJob.setupTask(context)
                    }
                    if (oldVersion < 102) {
                        // This was accidentally visible from the reader settings sheet, but should always
                        // be disabled in release builds.
                        if (isReleaseBuildType) {
                            readerPreferences.longStripSplitWebtoon().set(false)
                        }
                    }
                    return true
                }
            }
        }
        return false
    }
}
