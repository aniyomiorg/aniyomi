package eu.kanade.domain.category.interactor

import eu.kanade.domain.category.model.CategoryUpdate
import eu.kanade.domain.category.repository.CategoryRepositoryAnime
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext

class UpdateAnimeCategory(
    private val categoryRepository: CategoryRepositoryAnime,
) {

    suspend fun await(payload: CategoryUpdate): Result = withNonCancellableContext {
        try {
            categoryRepository.updatePartial(payload)
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
