package mihon.core.migration.migrations

import eu.kanade.domain.source.service.SourcePreferences
import logcat.LogPriority
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.domain.extension.anime.repository.AnimeExtensionStoreRepository
import mihon.domain.extensionrepo.exception.SaveExtensionRepoException
import mihon.domain.extensionrepo.manga.repository.MangaExtensionRepoRepository
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat

class TrustExtensionRepositoryMigration : Migration {
    override val version: Float = 7f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val sourcePreferences = migrationContext.get<SourcePreferences>() ?: return@withIOContext false

        val animeRepository = migrationContext.get<AnimeExtensionStoreRepository>() ?: return@withIOContext false
        for ((index, source) in sourcePreferences.animeExtensionRepos().get().withIndex()) {
            try {
                animeRepository.insertFromPreference(
                    indexUrl = source.removeSuffix("/index.min.json").removeSuffix("/index.json") + "/repo.json",
                    name = "Repo #${index + 1}",
                )
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Error Migrating Extension Repo with baseUrl: $source" }
            }
        }
        sourcePreferences.animeExtensionRepos().delete()

        val mangaExtensionRepositoryRepository =
            migrationContext.get<MangaExtensionRepoRepository>() ?: return@withIOContext false
        for ((index, source) in sourcePreferences.mangaExtensionRepos().get().withIndex()) {
            try {
                mangaExtensionRepositoryRepository.upsertRepo(
                    source,
                    "Repo #${index + 1}",
                    null,
                    source,
                    "NOFINGERPRINT-${index + 1}",
                )
            } catch (e: SaveExtensionRepoException) {
                logcat(LogPriority.ERROR, e) {
                    "Error Migrating Manga Extension Repo with baseUrl: $source"
                }
            }
        }
        sourcePreferences.mangaExtensionRepos().delete()

        return@withIOContext true
    }
}
