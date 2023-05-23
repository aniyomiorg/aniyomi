package eu.kanade.domain.category.manga.interactor

import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate

class RenameMangaCategory(
    private val categoryRepository: MangaCategoryRepository,
) {

    suspend fun await(categoryId: Long, name: String) = withNonCancellableContext {
        val update = CategoryUpdate(
            id = categoryId,
            name = name,
        )

        try {
            categoryRepository.updatePartialMangaCategory(update)
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
