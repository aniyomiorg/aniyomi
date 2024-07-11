package mihon.core.migration.migrations

import eu.kanade.domain.source.service.SourcePreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.getAndSet

class ExternalRepoMigration : Migration {
    override val version = 114f

    // Clean up external repos
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val sourcePreferences = migrationContext.get<SourcePreferences>() ?: return false

        sourcePreferences.mangaExtensionRepos().getAndSet {
            it.map { repo -> "https://raw.githubusercontent.com/$repo/repo" }.toSet()
        }

        return true
    }
}
