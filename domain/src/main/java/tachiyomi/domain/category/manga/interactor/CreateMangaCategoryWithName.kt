package tachiyomi.domain.category.manga.interactor

import logcat.LogPriority
import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences

class CreateMangaCategoryWithName(
    private val categoryRepository: MangaCategoryRepository,
    private val preferences: LibraryPreferences,
) {

    private val initialFlags: Long
        get() {
            val sort = preferences.libraryMangaSortingMode().get()
            return preferences.libraryDisplayMode().get().flag or
                sort.type.flag or
                sort.direction.flag
        }

    suspend fun await(name: String): Result = withNonCancellableContext {
        val categories = categoryRepository.getAllMangaCategories()
        val nextOrder = categories.maxOfOrNull { it.order }?.plus(1) ?: 0
        val newCategory = Category(
            id = 0,
            name = name,
            order = nextOrder,
            flags = initialFlags,
            hidden = false,
        )

        try {
            categoryRepository.insertMangaCategory(newCategory)
            Result.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.InternalError(e)
        }
    }

    sealed class Result {
        object Success : Result()
        data class InternalError(val error: Throwable) : Result()
    }
}
