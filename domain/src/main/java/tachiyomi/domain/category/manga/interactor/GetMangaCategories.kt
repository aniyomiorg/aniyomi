package tachiyomi.domain.category.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.Category

class GetMangaCategories(
    private val categoryRepository: MangaCategoryRepository,
) {
    fun subscribe(): Flow<List<Category>> {
        return categoryRepository.getAllMangaCategoriesAsFlow()
    }

    fun subscribe(mangaId: Long): Flow<List<Category>> {
        return categoryRepository.getCategoriesByMangaIdAsFlow(mangaId)
    }

    suspend fun await(): List<Category> {
        return categoryRepository.getAllMangaCategories()
    }

    suspend fun await(mangaId: Long): List<Category> {
        return categoryRepository.getCategoriesByMangaId(mangaId)
    }
}
