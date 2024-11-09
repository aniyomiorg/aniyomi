package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.Preference

class PermaTrustExtensionsMigration : Migration {
    override val version = 117f

    // Allow permanently trusting unofficial extensions by version code + signature
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        prefs.edit {
            remove(Preference.appStateKey("trusted_signatures"))
        }

        return true
    }
}
