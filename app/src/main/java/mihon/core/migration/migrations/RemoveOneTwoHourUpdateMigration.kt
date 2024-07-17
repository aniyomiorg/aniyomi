package mihon.core.migration.migrations

import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.domain.library.service.LibraryPreferences

class RemoveOneTwoHourUpdateMigration : Migration {
    override val version = 61f

    // Handle removed every 1 or 2 hour library updates
    @Suppress("MagicNumber")
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<App>() ?: return false
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return false

        val updateInterval = libraryPreferences.autoUpdateInterval().get()
        if (updateInterval == 1 || updateInterval == 2) {
            libraryPreferences.autoUpdateInterval().set(3)
            MangaLibraryUpdateJob.setupTask(context, 3)
            AnimeLibraryUpdateJob.setupTask(context, 3)
        }

        return true
    }
}
