package tachiyomi.data.category.anime

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.mi.data.AnimeDatabase

class AnimeCategoryRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeCategoryRepository {

    override suspend fun getAnimeCategory(id: Long): Category? {
        return handler.awaitOneOrNull { categoriesQueries.getCategory(id, ::mapCategory) }
    }

    override suspend fun getAllAnimeCategories(): List<Category> {
        return handler.awaitList { categoriesQueries.getCategories(::mapCategory) }
    }

    override suspend fun getAllVisibleAnimeCategories(): List<Category> {
        return handler.awaitList { categoriesQueries.getVisibleCategories(::mapCategory) }
    }

    override fun getAllAnimeCategoriesAsFlow(): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getCategories(::mapCategory) }
    }

    override fun getAllVisibleAnimeCategoriesAsFlow(): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getVisibleCategories(::mapCategory) }
    }

    override suspend fun getCategoriesByAnimeId(animeId: Long): List<Category> {
        return handler.awaitList {
            categoriesQueries.getCategoriesByAnimeId(animeId, ::mapCategory)
        }
    }

    override suspend fun getVisibleCategoriesByAnimeId(animeId: Long): List<Category> {
        return handler.awaitList {
            categoriesQueries.getVisibleCategoriesByAnimeId(animeId, ::mapCategory)
        }
    }

    override fun getCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>> {
        return handler.subscribeToList {
            categoriesQueries.getCategoriesByAnimeId(animeId, ::mapCategory)
        }
    }

    override fun getVisibleCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>> {
        return handler.subscribeToList {
            categoriesQueries.getVisibleCategoriesByAnimeId(animeId, ::mapCategory)
        }
    }

    override suspend fun insertAnimeCategory(category: Category) {
        handler.await {
            categoriesQueries.insert(
                name = category.name,
                order = category.order,
                flags = category.flags,
                parentId = category.parentId,
                thumbnailUrl = category.thumbnailUrl,
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
            hidden = update.hidden?.let { if (it) 1L else 0L },
            parentId = update.parentId,
            thumbnailUrl = update.thumbnailUrl,
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

    override suspend fun clearAnimeCategoryParentId(categoryId: Long, order: Long) {
        handler.await {
            categoriesQueries.clearParentId(
                categoryId = categoryId,
                order = order,
            )
        }
    }

    override suspend fun getChildAnimeCategories(parentId: Long): List<Category> {
        return handler.awaitList { categoriesQueries.getChildCategories(parentId, ::mapCategory) }
    }

    override suspend fun getRootAnimeCategories(): List<Category> {
        return handler.awaitList { categoriesQueries.getRootCategories(::mapCategory) }
    }

    override fun getChildAnimeCategoriesAsFlow(parentId: Long): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getChildCategories(parentId, ::mapCategory) }
    }

    override fun getRootAnimeCategoriesAsFlow(): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getRootCategories(::mapCategory) }
    }

    override suspend fun getEntriesInCategory(categoryId: Long): List<Pair<Long, Long>> {
        return handler.awaitList { animes_categoriesQueries.getEntriesInCategory(categoryId) }
            .map { (animeId, sortOrder) -> Pair(animeId, sortOrder) }
    }

    override suspend fun updateSortOrder(animeId: Long, categoryId: Long, sortOrder: Long) {
        handler.await {
            animes_categoriesQueries.updateSortOrder(animeId, categoryId, sortOrder)
        }
    }

    private fun mapCategory(
        id: Long,
        name: String,
        order: Long,
        flags: Long,
        hidden: Long,
        parentId: Long?,
        thumbnailUrl: String?,
    ): Category {
        return Category(
            id = id,
            name = name,
            order = order,
            flags = flags,
            hidden = hidden == 1L,
            parentId = parentId,
            thumbnailUrl = thumbnailUrl,
        )
    }
}
