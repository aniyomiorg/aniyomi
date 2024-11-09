package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class PlayerPreferenceMigration : Migration {
    override val version = 92f

    // add migration for player preference
    @Suppress("SwallowedException")
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val playerPreferences = migrationContext.get<PlayerPreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        if (playerPreferences.progressPreference().isSet()) {
            prefs.edit {
                val progressString = try {
                    prefs.getString(playerPreferences.progressPreference().key(), null)
                } catch (e: ClassCastException) {
                    null
                } ?: return@edit
                val newProgress = progressString.toFloatOrNull() ?: return@edit
                putFloat(playerPreferences.progressPreference().key(), newProgress)
            }
        }

        return true
    }
}
