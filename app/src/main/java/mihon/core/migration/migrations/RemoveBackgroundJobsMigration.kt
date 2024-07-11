package mihon.core.migration.migrations

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.util.system.workManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class RemoveBackgroundJobsMigration : Migration {
    override val version = 97f

    // Removed background jobs
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<App>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        context.workManager.cancelAllWorkByTag("UpdateChecker")
        context.workManager.cancelAllWorkByTag("ExtensionUpdate")
        prefs.edit {
            remove("automatic_ext_updates")
        }

        return true
    }
}
