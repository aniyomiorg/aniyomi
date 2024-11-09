package mihon.core.migration.migrations

import android.app.Application
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import java.io.File

class DeleteExternalChapterCacheDirMigration : Migration {
    override val version = 26f

    // Delete external chapter cache dir.
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false

        val extCache = context.externalCacheDir
        if (extCache != null) {
            val chapterCache = File(extCache, "chapter_disk_cache")
            if (chapterCache.exists()) {
                chapterCache.deleteRecursively()
            }
        }

        return true
    }
}
