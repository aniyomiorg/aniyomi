package eu.kanade.domain.category.interactor

import eu.kanade.domain.category.repository.CategoryRepositoryAnime

class DeleteCategoryAnime(
    private val categoryRepository: CategoryRepositoryAnime,
) {

    suspend fun await(categoryId: Long) {
        categoryRepository.delete(categoryId)
    }
}
