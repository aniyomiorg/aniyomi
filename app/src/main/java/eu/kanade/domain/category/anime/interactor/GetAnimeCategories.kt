package eu.kanade.domain.category.anime.interactor

import eu.kanade.domain.category.anime.repository.AnimeCategoryRepository
import eu.kanade.domain.category.model.Category
import kotlinx.coroutines.flow.Flow

class GetAnimeCategories(
    private val categoryRepository: AnimeCategoryRepository,
) {

    fun subscribe(): Flow<List<Category>> {
        return categoryRepository.getAllAnimeCategoriesAsFlow()
    }

    fun subscribe(animeId: Long): Flow<List<Category>> {
        return categoryRepository.getCategoriesByAnimeIdAsFlow(animeId)
    }

    suspend fun await(): List<Category> {
        return categoryRepository.getAllAnimeCategories()
    }

    suspend fun await(animeId: Long): List<Category> {
        return categoryRepository.getCategoriesByAnimeId(animeId)
    }
}
