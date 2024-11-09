package mihon.core.migration.migrations

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.domain.library.service.LibraryPreferences

class MigrateToTriStateMigration : Migration {
    override val version = 52f

    // Migrate library filters to tri-state versions
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        prefs.edit {
            putInt(
                libraryPreferences.filterDownloadedManga().key(),
                convertBooleanPrefToTriState(prefs, "pref_filter_downloaded_key"),
            )
            remove("pref_filter_downloaded_key")

            putInt(
                libraryPreferences.filterUnread().key(),
                convertBooleanPrefToTriState(prefs, "pref_filter_unread_key"),
            )
            remove("pref_filter_unread_key")

            putInt(
                libraryPreferences.filterCompletedManga().key(),
                convertBooleanPrefToTriState(prefs, "pref_filter_completed_key"),
            )
            remove("pref_filter_completed_key")
        }

        return true
    }

    private fun convertBooleanPrefToTriState(prefs: SharedPreferences, key: String): Int {
        val oldPrefValue = prefs.getBoolean(key, false)
        return if (oldPrefValue) {
            1
        } else {
            0
        }
    }
}
