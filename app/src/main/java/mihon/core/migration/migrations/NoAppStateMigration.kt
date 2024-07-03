package mihon.core.migration.migrations

import eu.kanade.tachiyomi.App
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.core.migration.replacePreferences
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class NoAppStateMigration : Migration {
    override val version = 113f

    // Don't include "app state" preferences in backups
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<App>() ?: return false
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return false

        val prefsToReplace = listOf(
            "pref_download_only",
            "incognito_mode",
            "last_catalogue_source",
            "trusted_signatures",
            "last_app_closed",
            "library_update_last_timestamp",
            "library_unseen_updates_count",
            "last_used_category",
            "last_app_check",
            "last_ext_check",
            "last_version_code",
            "storage_dir",
        )
        replacePreferences(
            preferenceStore = preferenceStore,
            filterPredicate = { it.key in prefsToReplace },
            newKey = { Preference.appStateKey(it) },
        )

        // Deleting old download cache index files, but might as well clear it all out
        context.cacheDir.deleteRecursively()

        return true
    }
}
