package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class ResetRotationMigration : Migration {
    override val version = 59f

    // Reset rotation to Free after replacing Lock
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        if (prefs.contains("pref_rotation_type_key")) {
            prefs.edit {
                putInt("pref_rotation_type_key", 1)
            }
        }

        return true
    }
}
