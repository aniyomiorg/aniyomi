package tachiyomi.domain.category.anime.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences

class DeleteAnimeCategory(
    private val categoryRepository: AnimeCategoryRepository,
    private val libraryPreferences: LibraryPreferences,
    private val downloadPreferences: DownloadPreferences,
) {

    suspend fun await(categoryId: Long) = withNonCancellableContext {
        val allCategories = categoryRepository.getAllAnimeCategories()

        fun getAllChildIds(parentId: Long): List<Long> {
            val children = allCategories.filter { it.parentId == parentId }
            return children.flatMap { child ->
                listOf(child.id) + getAllChildIds(child.id)
            }
        }

        val categoryIdsToDelete = listOf(categoryId).plus(getAllChildIds(categoryId)).toSet()

        for (id in categoryIdsToDelete) {
            try {
                categoryRepository.deleteAnimeCategory(id)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                return@withNonCancellableContext Result.InternalError(e)
            }
        }

        val remainingCategories = categoryRepository.getAllAnimeCategories()
        val updates = remainingCategories.mapIndexed { index, category ->
            CategoryUpdate(
                id = category.id,
                order = index.toLong(),
            )
        }

        val defaultCategory = libraryPreferences.defaultAnimeCategory().get()
        if (categoryIdsToDelete.any { it.toInt() == defaultCategory }) {
            libraryPreferences.defaultAnimeCategory().delete()
        }

        val categoryPreferences = listOf(
            libraryPreferences.animeUpdateCategories(),
            libraryPreferences.animeUpdateCategoriesExclude(),
            downloadPreferences.removeExcludeAnimeCategories(),
            downloadPreferences.downloadNewEpisodeCategories(),
            downloadPreferences.downloadNewEpisodeCategoriesExclude(),
        )
        categoryPreferences.forEach { preference ->
            val ids = preference.get()
            val newIds = ids.filterNot { it.toLong() in categoryIdsToDelete }.toSet()
            if (newIds.size != ids.size) {
                preference.set(newIds)
            }
        }

        try {
            categoryRepository.updatePartialAnimeCategories(updates)
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
