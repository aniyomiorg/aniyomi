package tachiyomi.domain.category.manga.interactor

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate

class ReorderMangaCategory(
    private val categoryRepository: MangaCategoryRepository,
) {

    private val mutex = Mutex()

    suspend fun await(category: Category, newIndex: Int) = withNonCancellableContext {
        mutex.withLock {
            val categories = categoryRepository.getAllMangaCategories()
                .filterNot(Category::isSystemCategory)
                .toMutableList()

            val currentIndex = categories.indexOfFirst { it.id == category.id }
            if (currentIndex == -1) {
                return@withNonCancellableContext Result.Unchanged
            }

            try {
                categories.add(newIndex, categories.removeAt(currentIndex))

                val updates = categories.mapIndexed { index, category ->
                    CategoryUpdate(
                        id = category.id,
                        order = index.toLong(),
                    )
                }

                categoryRepository.updatePartialMangaCategories(updates)
                Result.Success
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                Result.InternalError(e)
            }
        }
    }

    sealed interface Result {
        data object Success : Result
        data object Unchanged : Result
        data class InternalError(val error: Throwable) : Result
    }
}
