package tachiyomi.domain.category.anime.interactor

import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.category.model.CategoryUpdate

class UpdateAnimeCategory(
    private val categoryRepository: AnimeCategoryRepository,
) {

    suspend fun await(payload: CategoryUpdate): Result = withNonCancellableContext {
        try {
            categoryRepository.updatePartialAnimeCategory(payload)
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
