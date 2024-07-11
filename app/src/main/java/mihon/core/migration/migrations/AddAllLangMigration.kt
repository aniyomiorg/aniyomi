package mihon.core.migration.migrations

import eu.kanade.domain.source.service.SourcePreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.plusAssign

class AddAllLangMigration : Migration {
    override val version = 70f

    // Migration to add "all" to enabled langauges
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val sourcePreferences = migrationContext.get<SourcePreferences>() ?: return false

        if (sourcePreferences.enabledLanguages().isSet()) {
            sourcePreferences.enabledLanguages() += "all"
        }

        return true
    }
}
