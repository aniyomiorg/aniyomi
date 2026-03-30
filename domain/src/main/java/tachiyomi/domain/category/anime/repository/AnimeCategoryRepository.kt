package tachiyomi.domain.category.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate

interface AnimeCategoryRepository {

    suspend fun getAnimeCategory(id: Long): Category?

    suspend fun getAllAnimeCategories(): List<Category>

    suspend fun getAllVisibleAnimeCategories(): List<Category>

    suspend fun getChildAnimeCategories(parentId: Long): List<Category>

    suspend fun getRootAnimeCategories(): List<Category>

    fun getAllAnimeCategoriesAsFlow(): Flow<List<Category>>

    fun getAllVisibleAnimeCategoriesAsFlow(): Flow<List<Category>>

    fun getChildAnimeCategoriesAsFlow(parentId: Long): Flow<List<Category>>

    fun getRootAnimeCategoriesAsFlow(): Flow<List<Category>>

    suspend fun getCategoriesByAnimeId(animeId: Long): List<Category>

    suspend fun getVisibleCategoriesByAnimeId(animeId: Long): List<Category>

    fun getCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>>

    fun getVisibleCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>>

    suspend fun insertAnimeCategory(category: Category)

    suspend fun updatePartialAnimeCategory(update: CategoryUpdate)

    suspend fun updatePartialAnimeCategories(updates: List<CategoryUpdate>)

    suspend fun updateAllAnimeCategoryFlags(flags: Long?)

    suspend fun deleteAnimeCategory(categoryId: Long)

    suspend fun clearAnimeCategoryParentId(categoryId: Long, order: Long)

    suspend fun getEntriesInCategory(categoryId: Long): List<Pair<Long, Long>>

    suspend fun updateSortOrder(animeId: Long, categoryId: Long, sortOrder: Long)
}
