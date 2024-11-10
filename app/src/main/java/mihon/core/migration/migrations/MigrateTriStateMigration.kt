package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.data.track.TrackerManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum

class MigrateTriStateMigration : Migration {
    override val version = 99f

    // Migrate TriState usages to TriStateFilter enum
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val trackerManager = migrationContext.get<TrackerManager>() ?: return false
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val prefKeys = listOf(
            "pref_filter_library_downloaded",
            "pref_filter_library_unread",
            "pref_filter_library_unseen",
            "pref_filter_library_started",
            "pref_filter_library_bookmarked",
            "pref_filter_library_completed",
        ) + trackerManager.trackers.map { "pref_filter_library_tracked_${it.id}" }

        prefKeys.forEach { key ->
            val pref = preferenceStore.getInt(key, 0)
            prefs.edit {
                remove(key)

                val newValue = when (pref.get()) {
                    1 -> TriState.ENABLED_IS
                    2 -> TriState.ENABLED_NOT
                    else -> TriState.DISABLED
                }

                preferenceStore.getEnum("${key}_v2", TriState.DISABLED).set(
                    newValue,
                )
            }
        }

        return true
    }
}
