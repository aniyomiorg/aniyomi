package mihon.core.migration.migrations

import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class UseWorkManagerMigration : Migration {
    override val version = 96f

    // Fully utilize WorkManager for library updates
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<App>() ?: return false

        MangaLibraryUpdateJob.cancelAllWorks(context)
        AnimeLibraryUpdateJob.cancelAllWorks(context)
        MangaLibraryUpdateJob.setupTask(context)
        AnimeLibraryUpdateJob.setupTask(context)

        return true
    }
}
