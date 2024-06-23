package tachiyomi.domain.category.anime.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences

class CreateAnimeCategoryWithName(
    private val categoryRepository: AnimeCategoryRepository,
    private val preferences: LibraryPreferences,
) {

    private val initialFlags: Long
        get() {
            val sort = preferences.animeSortingMode().get()
            return sort.type.flag or sort.direction.flag
        }

    suspend fun await(name: String): Result = withNonCancellableContext {
        val categories = categoryRepository.getAllAnimeCategories()
        val nextOrder = categories.maxOfOrNull { it.order }?.plus(1) ?: 0
        val newCategory = Category(
            id = 0,
            name = name,
            order = nextOrder,
            flags = initialFlags,
            hidden = false,
        )

        try {
            categoryRepository.insertAnimeCategory(newCategory)
            Result.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.InternalError(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data class InternalError(val error: Throwable) : Result
    }
}
