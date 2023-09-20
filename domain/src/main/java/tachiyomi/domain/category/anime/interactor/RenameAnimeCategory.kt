package tachiyomi.domain.category.anime.interactor

import logcat.LogPriority
import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate

class RenameAnimeCategory(
    private val categoryRepository: AnimeCategoryRepository,
) {

    suspend fun await(categoryId: Long, name: String) = withNonCancellableContext {
        val update = CategoryUpdate(
            id = categoryId,
            name = name,
        )

        try {
            categoryRepository.updatePartialAnimeCategory(update)
            Result.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.InternalError(e)
        }
    }

    suspend fun await(category: Category, name: String) = await(category.id, name)

    sealed class Result {
        object Success : Result()
        data class InternalError(val error: Throwable) : Result()
    }
}
