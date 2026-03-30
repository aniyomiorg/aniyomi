package tachiyomi.domain.category.manga.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository

class ReorderMangaEntry(
    private val categoryRepository: MangaCategoryRepository,
) {
    suspend fun moveUp(mangaId: Long, categoryId: Long): Boolean {
        return try {
            val entries = categoryRepository.getEntriesInCategory(categoryId)
            val currentIndex = entries.indexOfFirst { it.first == mangaId }
            if (currentIndex <= 0) return false

            val currentOrder = entries[currentIndex].second
            val prevOrder = entries[currentIndex - 1].second

            categoryRepository.updateSortOrder(mangaId, categoryId, prevOrder)
            categoryRepository.updateSortOrder(entries[currentIndex - 1].first, categoryId, currentOrder)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    suspend fun moveDown(mangaId: Long, categoryId: Long): Boolean {
        return try {
            val entries = categoryRepository.getEntriesInCategory(categoryId)
            val currentIndex = entries.indexOfFirst { it.first == mangaId }
            if (currentIndex < 0 || currentIndex >= entries.size - 1) return false

            val currentOrder = entries[currentIndex].second
            val nextOrder = entries[currentIndex + 1].second

            categoryRepository.updateSortOrder(mangaId, categoryId, nextOrder)
            categoryRepository.updateSortOrder(entries[currentIndex + 1].first, categoryId, currentOrder)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }
}
