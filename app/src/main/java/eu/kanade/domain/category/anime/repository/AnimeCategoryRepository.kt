package eu.kanade.domain.category.anime.repository

import eu.kanade.domain.category.model.Category
import eu.kanade.domain.category.model.CategoryUpdate
import kotlinx.coroutines.flow.Flow

interface AnimeCategoryRepository {

    suspend fun getAnimeCategory(id: Long): Category?

    suspend fun getAllAnimeCategories(): List<Category>

    fun getAllAnimeCategoriesAsFlow(): Flow<List<Category>>

    suspend fun getCategoriesByAnimeId(animeId: Long): List<Category>

    fun getCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>>

    suspend fun insertAnimeCategory(category: Category)

    suspend fun updatePartialAnimeCategory(update: CategoryUpdate)

    suspend fun updatePartialAnimeCategories(updates: List<CategoryUpdate>)

    suspend fun updateAllAnimeCategoryFlags(flags: Long?)

    suspend fun deleteAnimeCategory(categoryId: Long)
}
