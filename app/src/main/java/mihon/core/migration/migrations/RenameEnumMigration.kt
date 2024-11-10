package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.domain.library.service.LibraryPreferences

class RenameEnumMigration : Migration {
    override val version = 81f

    // Handle renamed enum values
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        prefs.edit {
            val newMangaSortingMode = when (
                val oldSortingMode = prefs.getString(
                    libraryPreferences.mangaSortingMode().key(),
                    "ALPHABETICAL",
                )
            ) {
                "LAST_CHECKED" -> "LAST_MANGA_UPDATE"
                "UNREAD" -> "UNREAD_COUNT"
                "DATE_FETCHED" -> "CHAPTER_FETCH_DATE"
                else -> oldSortingMode
            }
            val newAnimeSortingMode = when (
                val oldSortingMode = prefs.getString(
                    libraryPreferences.animeSortingMode().key(),
                    "ALPHABETICAL",
                )
            ) {
                "LAST_CHECKED" -> "LAST_MANGA_UPDATE"
                "UNREAD" -> "UNREAD_COUNT"
                "DATE_FETCHED" -> "CHAPTER_FETCH_DATE"
                else -> oldSortingMode
            }
            putString(libraryPreferences.mangaSortingMode().key(), newMangaSortingMode)
            putString(libraryPreferences.animeSortingMode().key(), newAnimeSortingMode)
        }

        return true
    }
}
