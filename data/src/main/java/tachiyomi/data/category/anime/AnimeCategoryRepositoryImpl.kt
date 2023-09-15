package tachiyomi.data.category.anime

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.category.categoryMapper
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.mi.data.AnimeDatabase

class AnimeCategoryRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeCategoryRepository {

    override suspend fun getAnimeCategory(id: Long): Category? {
        return handler.awaitOneOrNull { categoriesQueries.getCategory(id, categoryMapper) }
    }

    override suspend fun getAllAnimeCategories(): List<Category> {
        return handler.awaitList { categoriesQueries.getCategories(categoryMapper) }
    }

    override suspend fun getAllVisibleAnimeCategories(): List<Category> {
        return handler.awaitList { categoriesQueries.getVisibleCategories(categoryMapper) }
    }

    override fun getAllAnimeCategoriesAsFlow(): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getCategories(categoryMapper) }
    }

    override fun getAllVisibleAnimeCategoriesAsFlow(): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getVisibleCategories(categoryMapper) }
    }

    override suspend fun getCategoriesByAnimeId(animeId: Long): List<Category> {
        return handler.awaitList {
            categoriesQueries.getCategoriesByAnimeId(animeId, categoryMapper)
        }
    }

    override suspend fun getVisibleCategoriesByAnimeId(animeId: Long): List<Category> {
        return handler.awaitList {
            categoriesQueries.getVisibleCategoriesByAnimeId(animeId, categoryMapper)
        }
    }

    override fun getCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>> {
        return handler.subscribeToList {
            categoriesQueries.getCategoriesByAnimeId(animeId, categoryMapper)
        }
    }

    override fun getVisibleCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>> {
        return handler.subscribeToList {
            categoriesQueries.getVisibleCategoriesByAnimeId(animeId, categoryMapper)
        }
    }

    override suspend fun insertAnimeCategory(category: Category) {
        handler.await {
            categoriesQueries.insert(
                name = category.name,
                order = category.order,
                flags = category.flags,
            )
        }
    }

    override suspend fun updatePartialAnimeCategory(update: CategoryUpdate) {
        handler.await {
            updatePartialBlocking(update)
        }
    }

    override suspend fun updatePartialAnimeCategories(updates: List<CategoryUpdate>) {
        handler.await(inTransaction = true) {
            for (update in updates) {
                updatePartialBlocking(update)
            }
        }
    }

    private fun AnimeDatabase.updatePartialBlocking(update: CategoryUpdate) {
        categoriesQueries.update(
            name = update.name,
            order = update.order,
            flags = update.flags,
            hidden = if (update.hidden == true) 1L else 0L,
            categoryId = update.id,
        )
    }

    override suspend fun updateAllAnimeCategoryFlags(flags: Long?) {
        handler.await {
            categoriesQueries.updateAllFlags(flags)
        }
    }

    override suspend fun deleteAnimeCategory(categoryId: Long) {
        handler.await {
            categoriesQueries.delete(
                categoryId = categoryId,
            )
        }
    }
}
