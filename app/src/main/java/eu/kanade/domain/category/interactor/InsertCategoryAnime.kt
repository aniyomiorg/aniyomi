package eu.kanade.domain.category.interactor

import eu.kanade.domain.category.repository.CategoryRepositoryAnime

class InsertCategoryAnime(
    private val categoryRepository: CategoryRepositoryAnime,
) {

    suspend fun await(name: String, order: Long): Result {
        return try {
            categoryRepository.insert(name, order)
            Result.Success
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    sealed class Result {
        object Success : Result()
        data class Error(val error: Exception) : Result()
    }
}
