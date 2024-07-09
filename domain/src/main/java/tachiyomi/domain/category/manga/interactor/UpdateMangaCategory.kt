package tachiyomi.domain.category.manga.interactor

import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.CategoryUpdate

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

    sealed interface Result {
        data object Success : Result
        data class Error(val error: Exception) : Result
    }
}
