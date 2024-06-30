package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.manga.interactor.GetMangaCategories
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CategoriesRestorer(
    private val animeHandler: AnimeDatabaseHandler = Injekt.get(),
    private val mangaHandler: MangaDatabaseHandler = Injekt.get(),
    private val getAnimeCategories: GetAnimeCategories = Injekt.get(),
    private val getMangaCategories: GetMangaCategories = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
) {

    suspend fun restoreAnimeCategories(backupCategories: List<BackupCategory>) {
        if (backupCategories.isNotEmpty()) {
            val dbCategories = getAnimeCategories.await()
            val dbCategoriesByName = dbCategories.associateBy { it.name }
            var nextOrder = dbCategories.maxOfOrNull { it.order }?.plus(1) ?: 0

            val categories = backupCategories
                .sortedBy { it.order }
                .distinctBy { it.name }
                .map {
                    val newOrder = nextOrder++
                    dbCategoriesByName[it.name]
                        ?: animeHandler.awaitOneExecutable {
                            categoriesQueries.insert(it.name, newOrder, it.flags)
                            categoriesQueries.selectLastInsertedRowId()
                        }.let { id -> it.toCategory(id).copy(order = newOrder) }
                }

            libraryPreferences.categorizedDisplaySettings().set(
                (dbCategories + categories)
                    .distinctBy { it.flags }
                    .size > 1,
            )
        }
    }

    suspend fun restoreMangaCategories(backupCategories: List<BackupCategory>) {
        if (backupCategories.isNotEmpty()) {
            val dbCategories = getMangaCategories.await()
            val dbCategoriesByName = dbCategories.associateBy { it.name }
            var nextOrder = dbCategories.maxOfOrNull { it.order }?.plus(1) ?: 0

            val categories = backupCategories
                .sortedBy { it.order }
                .distinctBy { it.name }
                .map {
                    val newOrder = nextOrder++
                    dbCategoriesByName[it.name]
                        ?: mangaHandler.awaitOneExecutable {
                            categoriesQueries.insert(it.name, newOrder, it.flags)
                            categoriesQueries.selectLastInsertedRowId()
                        }.let { id -> it.toCategory(id).copy(order = newOrder) }
                }

            libraryPreferences.categorizedDisplaySettings().set(
                (dbCategories + categories)
                    .distinctBy { it.flags }
                    .size > 1,
            )
        }
    }
}
