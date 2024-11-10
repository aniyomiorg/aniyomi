package mihon.core.migration.migrations

import android.app.Application
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class SetupBackgroundTasksMigration : Migration {
    override val version = 64f

    // Set up background tasks
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false

        MangaLibraryUpdateJob.setupTask(context)
        AnimeLibraryUpdateJob.setupTask(context)

        return true
    }
}
