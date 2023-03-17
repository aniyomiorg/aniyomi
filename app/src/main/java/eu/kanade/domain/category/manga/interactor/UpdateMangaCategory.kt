package eu.kanade.domain.category.manga.interactor

import eu.kanade.domain.category.manga.repository.MangaCategoryRepository
import eu.kanade.domain.category.model.CategoryUpdate
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext

class UpdateMangaCategory(
    private val categoryRepository: MangaCategoryRepository,
) {

    suspend fun await(payload: CategoryUpdate): Result = withNonCancellableContext {
        try {
            categoryRepository.updatePartialMangaCategory(payload)
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
