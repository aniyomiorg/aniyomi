package mihon.core.migration.migrations

import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.domain.library.service.LibraryPreferences

class RemoveQuickUpdateMigration : Migration {
    override val version = 71f

    // Handle removed every 3, 4, 6, and 8 hour library updates
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<App>() ?: return false
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return false

        val updateInterval = libraryPreferences.autoUpdateInterval().get()
        if (updateInterval in listOf(3, 4, 6, 8)) {
            libraryPreferences.autoUpdateInterval().set(12)
            MangaLibraryUpdateJob.setupTask(context, 12)
            AnimeLibraryUpdateJob.setupTask(context, 12)
        }

        return true
    }
}
