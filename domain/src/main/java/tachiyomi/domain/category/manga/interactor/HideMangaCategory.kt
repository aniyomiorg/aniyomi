package tachiyomi.domain.category.manga.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate

class HideMangaCategory(
    private val categoryRepository: MangaCategoryRepository,
) {

    suspend fun await(category: Category) = withNonCancellableContext {
        val update = CategoryUpdate(
            id = category.id,
            hidden = !category.hidden,
        )

        try {
            categoryRepository.updatePartialMangaCategory(update)
            Result.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.InternalError(e)
        }
    }

    sealed class Result {
        data object Success : Result()
        data class InternalError(val error: Throwable) : Result()
    }
}
