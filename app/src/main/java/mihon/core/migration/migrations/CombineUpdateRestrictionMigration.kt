package mihon.core.migration.migrations

import android.app.Application
import androidx.preference.PreferenceManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.minusAssign
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_COMPLETED

class CombineUpdateRestrictionMigration : Migration {
    override val version = 72f

    // Combine global update item restrictions
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val oldUpdateOngoingOnly = prefs.getBoolean(
            "pref_update_only_non_completed_key",
            true,
        )
        if (!oldUpdateOngoingOnly) {
            libraryPreferences.autoUpdateItemRestrictions() -= ENTRY_NON_COMPLETED
        }

        return true
    }
}
