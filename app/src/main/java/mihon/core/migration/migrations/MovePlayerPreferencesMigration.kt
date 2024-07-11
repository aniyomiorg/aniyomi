package mihon.core.migration.migrations

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class MovePlayerPreferencesMigration : Migration {
    override val version = 93f

    // more migrations for player prefs
    @Suppress("SwallowedException")
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<App>() ?: return false
        val playerPreferences = migrationContext.get<PlayerPreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        listOf(
            playerPreferences.defaultPlayerOrientationType(),
            playerPreferences.defaultPlayerOrientationLandscape(),
            playerPreferences.defaultPlayerOrientationPortrait(),
            playerPreferences.skipLengthPreference(),
        ).forEach { pref ->
            if (pref.isSet()) {
                prefs.edit {
                    val oldString = try {
                        prefs.getString(pref.key(), null)
                    } catch (e: ClassCastException) {
                        null
                    } ?: return@edit
                    val newInt = oldString.toIntOrNull() ?: return@edit
                    putInt(pref.key(), newInt)
                }
                val trackingQueuePref =
                    context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)
                trackingQueuePref.all.forEach {
                    val (_, lastChapterRead) = it.value.toString().split(":")
                    trackingQueuePref.edit {
                        remove(it.key)
                        putFloat(it.key, lastChapterRead.toFloat())
                    }
                }
            }
        }

        return true
    }
}
