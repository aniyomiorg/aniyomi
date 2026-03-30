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
            if (currentIndex <= 0) {
                logcat(LogPriority.DEBUG) {
                    "ReorderAnimeEntry.moveUp: Already at top or not found, animeId=$animeId, categoryId=$categoryId"
                }
                return true
            }

            val currentOrder = entries[currentIndex].second
            val prevOrder = entries[currentIndex - 1].second

            categoryRepository.updateSortOrder(animeId, categoryId, prevOrder)
            categoryRepository.updateSortOrder(entries[currentIndex - 1].first, categoryId, currentOrder)

            logcat(LogPriority.DEBUG) {
                "ReorderAnimeEntry.moveUp: Swapped animeId=$animeId with previous, categoryId=$categoryId"
            }
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
            if (currentIndex < 0 || currentIndex >= entries.size - 1) {
                logcat(LogPriority.DEBUG) {
                    "ReorderAnimeEntry.moveDown: Already at bottom or not found, animeId=$animeId, categoryId=$categoryId"
                }
                return true
            }

            val currentOrder = entries[currentIndex].second
            val nextOrder = entries[currentIndex + 1].second

            categoryRepository.updateSortOrder(animeId, categoryId, nextOrder)
            categoryRepository.updateSortOrder(entries[currentIndex + 1].first, categoryId, currentOrder)

            logcat(LogPriority.DEBUG) {
                "ReorderAnimeEntry.moveDown: Swapped animeId=$animeId with next, categoryId=$categoryId"
            }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    suspend fun moveTo(animeId: Long, categoryId: Long, newPosition: Int): Boolean {
        return try {
            val entries = categoryRepository.getEntriesInCategory(categoryId).toMutableList()
            val currentIndex = entries.indexOfFirst { it.first == animeId }
            if (currentIndex < 0) {
                logcat(LogPriority.DEBUG) {
                    "ReorderAnimeEntry.moveTo: Entry not found, animeId=$animeId, categoryId=$categoryId"
                }
                return false
            }

            val entry = entries.removeAt(currentIndex)
            val targetIndex = newPosition.coerceIn(0, entries.size)
            entries.add(targetIndex, entry)

            entries.forEachIndexed { index, (animeId, _) ->
                categoryRepository.updateSortOrder(animeId, categoryId, index.toLong())
            }

            logcat(LogPriority.DEBUG) {
                "ReorderAnimeEntry.moveTo: Moved animeId=$animeId to position $targetIndex, categoryId=$categoryId"
            }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }
}
