package mihon.core.migration.migrations

import android.app.Application
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class SetupMangaLibraryUpdateMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false

        MangaLibraryUpdateJob.setupTask(context)

        return true
    }
}
