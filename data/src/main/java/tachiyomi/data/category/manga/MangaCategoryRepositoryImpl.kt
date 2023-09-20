package tachiyomi.data.category.manga

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.category.categoryMapper
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate

class MangaCategoryRepositoryImpl(
    private val handler: MangaDatabaseHandler,
) : MangaCategoryRepository {

    override suspend fun getMangaCategory(id: Long): Category? {
        return handler.awaitOneOrNull { categoriesQueries.getCategory(id, categoryMapper) }
    }

    override suspend fun getAllMangaCategories(): List<Category> {
        return handler.awaitList { categoriesQueries.getCategories(categoryMapper) }
    }

    override suspend fun getAllVisibleMangaCategories(): List<Category> {
        return handler.awaitList { categoriesQueries.getVisibleCategories(categoryMapper) }
    }

    override fun getAllMangaCategoriesAsFlow(): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getCategories(categoryMapper) }
    }

    override fun getAllVisibleMangaCategoriesAsFlow(): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getVisibleCategories(categoryMapper) }
    }

    override suspend fun getCategoriesByMangaId(mangaId: Long): List<Category> {
        return handler.awaitList {
            categoriesQueries.getCategoriesByMangaId(mangaId, categoryMapper)
        }
    }

    override suspend fun getVisibleCategoriesByMangaId(mangaId: Long): List<Category> {
        return handler.awaitList {
            categoriesQueries.getVisibleCategoriesByMangaId(mangaId, categoryMapper)
        }
    }

    override fun getCategoriesByMangaIdAsFlow(mangaId: Long): Flow<List<Category>> {
        return handler.subscribeToList {
            categoriesQueries.getCategoriesByMangaId(mangaId, categoryMapper)
        }
    }

    override fun getVisibleCategoriesByMangaIdAsFlow(mangaId: Long): Flow<List<Category>> {
        return handler.subscribeToList {
            categoriesQueries.getVisibleCategoriesByMangaId(mangaId, categoryMapper)
        }
    }

    override suspend fun insertMangaCategory(category: Category) {
        handler.await {
            categoriesQueries.insert(
                name = category.name,
                order = category.order,
                flags = category.flags,
            )
        }
    }

    override suspend fun updatePartialMangaCategory(update: CategoryUpdate) {
        handler.await {
            updatePartialBlocking(update)
        }
    }

    override suspend fun updatePartialMangaCategories(updates: List<CategoryUpdate>) {
        handler.await(inTransaction = true) {
            for (update in updates) {
                updatePartialBlocking(update)
            }
        }
    }

    private fun Database.updatePartialBlocking(update: CategoryUpdate) {
        categoriesQueries.update(
            name = update.name,
            order = update.order,
            flags = update.flags,
            hidden = if (update.hidden == true) 1L else 0L,
            categoryId = update.id,
        )
    }

    override suspend fun updateAllMangaCategoryFlags(flags: Long?) {
        handler.await {
            categoriesQueries.updateAllFlags(flags)
        }
    }

    override suspend fun deleteMangaCategory(categoryId: Long) {
        handler.await {
            categoriesQueries.delete(
                categoryId = categoryId,
            )
        }
    }
}
