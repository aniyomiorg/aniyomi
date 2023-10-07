package tachiyomi.domain.category.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.Category

class GetVisibleMangaCategories(
    private val categoryRepository: MangaCategoryRepository,
) {
    fun subscribe(): Flow<List<Category>> {
        return categoryRepository.getAllVisibleMangaCategoriesAsFlow()
    }

    fun subscribe(mangaId: Long): Flow<List<Category>> {
        return categoryRepository.getVisibleCategoriesByMangaIdAsFlow(mangaId)
    }

    suspend fun await(): List<Category> {
        return categoryRepository.getAllVisibleMangaCategories()
    }

    suspend fun await(mangaId: Long): List<Category> {
        return categoryRepository.getVisibleCategoriesByMangaId(mangaId)
    }
}
