package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.manga.interactor.GetMangaCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences

class CategoryPreferencesCleanupMigration : Migration {
    override val version: Float = 129f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return@withIOContext false
        val downloadPreferences = migrationContext.get<DownloadPreferences>() ?: return@withIOContext false

        val getAnimeCategories = migrationContext.get<GetAnimeCategories>() ?: return@withIOContext false
        val getMangaCategories = migrationContext.get<GetMangaCategories>() ?: return@withIOContext false
        val allAnimeCategories = getAnimeCategories.await().map { it.id.toString() }.toSet()
        val allMangaCategories = getMangaCategories.await().map { it.id.toString() }.toSet()

        val defaultAnimeCategory = libraryPreferences.defaultAnimeCategory().get()
        if (defaultAnimeCategory.toString() !in allAnimeCategories) {
            libraryPreferences.defaultAnimeCategory().delete()
        }
        val defaultMangaCategory = libraryPreferences.defaultMangaCategory().get()
        if (defaultMangaCategory.toString() !in allMangaCategories) {
            libraryPreferences.defaultMangaCategory().delete()
        }

        val categoryPreferences = listOf(
            libraryPreferences.animeUpdateCategories(),
            libraryPreferences.mangaUpdateCategories(),
            libraryPreferences.animeUpdateCategoriesExclude(),
            libraryPreferences.mangaUpdateCategoriesExclude(),
            downloadPreferences.removeExcludeCategories(),
            downloadPreferences.removeExcludeAnimeCategories(),
            downloadPreferences.downloadNewChapterCategories(),
            downloadPreferences.downloadNewEpisodeCategories(),
            downloadPreferences.downloadNewChapterCategoriesExclude(),
            downloadPreferences.downloadNewEpisodeCategoriesExclude(),
        )
        categoryPreferences.forEach { preference ->
            val ids = preference.get()
            val garbageIds = ids
                .minus(allAnimeCategories)
                .minus(allMangaCategories)
            if (garbageIds.isEmpty()) return@forEach
            preference.set(ids.minus(garbageIds))
        }
        return@withIOContext true
    }
}
