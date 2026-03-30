package tachiyomi.domain.category.anime.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository

class ReorderAnimeEntry(
    private val categoryRepository: AnimeCategoryRepository,
) {
    suspend fun moveUp(animeId: Long, categoryId: Long): Boolean {
        return try {
            val entries = categoryRepository.getEntriesInCategory(categoryId)
            val currentIndex = entries.indexOfFirst { it.first == animeId }
            if (currentIndex <= 0) return false

            val currentOrder = entries[currentIndex].second
            val prevOrder = entries[currentIndex - 1].second

            categoryRepository.updateSortOrder(animeId, categoryId, prevOrder)
            categoryRepository.updateSortOrder(entries[currentIndex - 1].first, categoryId, currentOrder)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    suspend fun moveDown(animeId: Long, categoryId: Long): Boolean {
        return try {
            val entries = categoryRepository.getEntriesInCategory(categoryId)
            val currentIndex = entries.indexOfFirst { it.first == animeId }
            if (currentIndex < 0 || currentIndex >= entries.size - 1) return false

            val currentOrder = entries[currentIndex].second
            val nextOrder = entries[currentIndex + 1].second

            categoryRepository.updateSortOrder(animeId, categoryId, nextOrder)
            categoryRepository.updateSortOrder(entries[currentIndex + 1].first, categoryId, currentOrder)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }
}
