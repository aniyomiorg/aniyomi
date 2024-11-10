package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class DOHMigration : Migration {
    override val version = 57f

    // Migrate DNS over HTTPS setting
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val networkPreferences = migrationContext.get<NetworkPreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val wasDohEnabled = prefs.getBoolean("enable_doh", false)
        if (wasDohEnabled) {
            prefs.edit {
                putInt(networkPreferences.dohProvider().key(), PREF_DOH_CLOUDFLARE)
                remove("enable_doh")
            }
        }

        return true
    }
}
