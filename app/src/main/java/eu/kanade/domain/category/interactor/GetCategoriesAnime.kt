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

    fun subscribe(animeId: Long): Flow<List<Category>> {
        return categoryRepository.getCategoriesByAnimeIdAsFlow(animeId)
    }

    suspend fun await(animeId: Long): List<Category> {
        return categoryRepository.getCategoriesByAnimeId(animeId)
    }
}
