package eu.kanade.domain.category.manga.repository

import eu.kanade.domain.category.model.Category
import eu.kanade.domain.category.model.CategoryUpdate
import kotlinx.coroutines.flow.Flow

interface MangaCategoryRepository {

    suspend fun getMangaCategory(id: Long): Category?

    suspend fun getAllMangaCategories(): List<Category>

    fun getAllMangaCategoriesAsFlow(): Flow<List<Category>>

    suspend fun getCategoriesByMangaId(mangaId: Long): List<Category>

    fun getCategoriesByMangaIdAsFlow(mangaId: Long): Flow<List<Category>>

    suspend fun insertMangaCategory(category: Category)

    suspend fun updatePartialMangaCategory(update: CategoryUpdate)

    suspend fun updatePartialMangaCategories(updates: List<CategoryUpdate>)

    suspend fun updateAllMangaCategoryFlags(flags: Long?)

    suspend fun deleteMangaCategory(categoryId: Long)
}
