package eu.kanade.domain.category.anime.interactor

import eu.kanade.domain.category.anime.repository.AnimeCategoryRepository
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.category.model.CategoryUpdate
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

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
