package mihon.core.migration.migrations

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.App
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.domain.library.service.LibraryPreferences

class MigrateSortingModeMigration : Migration {
    override val version = 64f

    // Switch to sort per category
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<App>() ?: return false
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val oldMangaSortingMode = prefs.getInt(
            libraryPreferences.mangaSortingMode().key(),
            0,
        )
        val oldAnimeSortingMode = prefs.getInt(
            libraryPreferences.animeSortingMode().key(),
            0,
        )
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
            remove(libraryPreferences.mangaSortingMode().key())
            remove(libraryPreferences.animeSortingMode().key())
            remove("library_sorting_ascending")
        }

        prefs.edit {
            putString(libraryPreferences.mangaSortingMode().key(), newMangaSortingMode)
            putString(libraryPreferences.animeSortingMode().key(), newAnimeSortingMode)
            putString("library_sorting_ascending", newSortingDirection)
        }

        return true
    }
}
