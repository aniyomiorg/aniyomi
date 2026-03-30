package tachiyomi.domain.category.manga.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entries.manga.model.MangaUpdate
import tachiyomi.domain.entries.manga.repository.MangaRepository
import tachiyomi.domain.library.service.LibraryPreferences

class DeleteMangaCategory(
    private val categoryRepository: MangaCategoryRepository,
    private val mangaRepository: MangaRepository,
    private val libraryPreferences: LibraryPreferences,
    private val downloadPreferences: DownloadPreferences,
) {

    suspend fun await(categoryId: Long) = withNonCancellableContext {
        val allCategories = categoryRepository.getAllMangaCategories()

        fun getAllChildIds(parentId: Long): List<Long> {
            val children = allCategories.filter { it.parentId == parentId }
            return children.flatMap { child ->
                listOf(child.id) + getAllChildIds(child.id)
            }
        }

        val categoryIdsToDelete = listOf(categoryId).plus(getAllChildIds(categoryId)).toSet()

        val allLibraryManga = mangaRepository.getLibraryManga()
        val mangaIdsInCategories = allLibraryManga
            .filter { it.category in categoryIdsToDelete }
            .map { it.id }

        if (mangaIdsInCategories.isNotEmpty()) {
            val updates = mangaIdsInCategories.map { id ->
                MangaUpdate(id = id, favorite = false)
            }
            try {
                mangaRepository.updateAllManga(updates)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
            }
        }

        for (id in categoryIdsToDelete) {
            try {
                categoryRepository.deleteMangaCategory(id)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                return@withNonCancellableContext Result.InternalError(e)
            }
        }

        val remainingCategories = categoryRepository.getAllMangaCategories()
        val updates = remainingCategories.mapIndexed { index, category ->
            CategoryUpdate(
                id = category.id,
                order = index.toLong(),
            )
        }

        val defaultCategory = libraryPreferences.defaultMangaCategory().get()
        if (categoryIdsToDelete.any { it.toInt() == defaultCategory }) {
            libraryPreferences.defaultMangaCategory().delete()
        }

        val categoryPreferences = listOf(
            libraryPreferences.mangaUpdateCategories(),
            libraryPreferences.mangaUpdateCategoriesExclude(),
            downloadPreferences.removeExcludeCategories(),
            downloadPreferences.downloadNewChapterCategories(),
            downloadPreferences.downloadNewChapterCategoriesExclude(),
        )
        categoryPreferences.forEach { preference ->
            val ids = preference.get()
            val newIds = ids.filterNot { it.toLong() in categoryIdsToDelete }.toSet()
            if (newIds.size != ids.size) {
                preference.set(newIds)
            }
        }

        try {
            categoryRepository.updatePartialMangaCategories(updates)
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
