package mihon.core.migration.migrations

import android.app.Application
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class RemoveReaderTapMigration : Migration {
    override val version = 77f

    // Remove reader tapping option in favor of disabled nav layouts
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val readerPreferences = migrationContext.get<ReaderPreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val oldReaderTap = prefs.getBoolean("reader_tap", false)
        if (!oldReaderTap) {
            readerPreferences.navigationModePager().set(5)
            readerPreferences.navigationModeWebtoon().set(5)
        }

        return true
    }
}
