package mihon.core.migration.migrations

import eu.kanade.domain.ui.UiPreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.PreferenceStore

class RelativeTimestampMigration : Migration {
    override val version = 106f

    // Bring back simplified relative timestamp setting
    @Suppress("MagicNumber")
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return false
        val uiPreferences = migrationContext.get<UiPreferences>() ?: return false

        val pref = preferenceStore.getInt("relative_time", 7)
        if (pref.get() == 0) {
            uiPreferences.relativeTime().set(false)
        }

        return true
    }
}
