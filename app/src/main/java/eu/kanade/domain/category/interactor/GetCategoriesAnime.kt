package eu.kanade.domain.category.interactor

import eu.kanade.domain.category.model.Category
import eu.kanade.domain.category.repository.CategoryRepositoryAnime
import kotlinx.coroutines.flow.Flow

class GetCategoriesAnime(
    private val categoryRepository: CategoryRepositoryAnime,
) {

    fun subscribe(): Flow<List<Category>> {
        return categoryRepository.getAll()
    }

    suspend fun await(animeId: Long): List<Category> {
        return categoryRepository.getCategoriesForAnime(animeId)
    }
}
