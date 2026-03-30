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
            if (currentIndex <= 0) {
                logcat(LogPriority.DEBUG) {
                    "ReorderMangaEntry.moveUp: Already at top or not found, mangaId=$mangaId, categoryId=$categoryId"
                }
                return true
            }

            val currentOrder = entries[currentIndex].second
            val prevOrder = entries[currentIndex - 1].second

            categoryRepository.updateSortOrder(mangaId, categoryId, prevOrder)
            categoryRepository.updateSortOrder(entries[currentIndex - 1].first, categoryId, currentOrder)

            logcat(LogPriority.DEBUG) {
                "ReorderMangaEntry.moveUp: Swapped mangaId=$mangaId with previous, categoryId=$categoryId"
            }
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
            if (currentIndex < 0 || currentIndex >= entries.size - 1) {
                logcat(LogPriority.DEBUG) {
                    "ReorderMangaEntry.moveDown: Already at bottom or not found, mangaId=$mangaId, categoryId=$categoryId"
                }
                return true
            }

            val currentOrder = entries[currentIndex].second
            val nextOrder = entries[currentIndex + 1].second

            categoryRepository.updateSortOrder(mangaId, categoryId, nextOrder)
            categoryRepository.updateSortOrder(entries[currentIndex + 1].first, categoryId, currentOrder)

            logcat(LogPriority.DEBUG) {
                "ReorderMangaEntry.moveDown: Swapped mangaId=$mangaId with next, categoryId=$categoryId"
            }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    suspend fun moveTo(mangaId: Long, categoryId: Long, newPosition: Int): Boolean {
        return try {
            val entries = categoryRepository.getEntriesInCategory(categoryId).toMutableList()
            val currentIndex = entries.indexOfFirst { it.first == mangaId }
            if (currentIndex < 0) {
                logcat(LogPriority.DEBUG) {
                    "ReorderMangaEntry.moveTo: Entry not found, mangaId=$mangaId, categoryId=$categoryId"
                }
                return false
            }

            val entry = entries.removeAt(currentIndex)
            val targetIndex = newPosition.coerceIn(0, entries.size)
            entries.add(targetIndex, entry)

            entries.forEachIndexed { index, (mangaId, _) ->
                categoryRepository.updateSortOrder(mangaId, categoryId, index.toLong())
            }

            logcat(LogPriority.DEBUG) {
                "ReorderMangaEntry.moveTo: Moved mangaId=$mangaId to position $targetIndex, categoryId=$categoryId"
            }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }
}
