package tachiyomi.domain.source.anime.interactor

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.anime.model.FeedSavedSearch
import tachiyomi.domain.source.anime.model.FeedSavedSearchUpdate
import tachiyomi.domain.source.anime.repository.FeedSavedSearchRepository
import java.util.Collections

class ReorderFeed(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    private val mutex = Mutex()

    suspend fun moveUp(feed: FeedSavedSearch, global: Boolean = true): Result = awaitGlobal(feed, MoveTo.UP, global)

    suspend fun moveDown(feed: FeedSavedSearch, global: Boolean = true): Result = awaitGlobal(feed, MoveTo.DOWN, global)

    private suspend fun awaitGlobal(
        feed: FeedSavedSearch,
        moveTo: MoveTo,
        global: Boolean = true,
    ) = withNonCancellableContext {
        mutex.withLock {
            val feeds = if (global) {
                feedSavedSearchRepository.getGlobal()
                    .toMutableList()
            } else {
                feedSavedSearchRepository.getBySourceId(feed.source)
                    .toMutableList()
            }

            val currentIndex = feeds.indexOfFirst { it.id == feed.id }
            if (currentIndex == -1) {
                return@withNonCancellableContext Result.Unchanged
            }

            val newPosition = when (moveTo) {
                MoveTo.UP -> currentIndex - 1
                MoveTo.DOWN -> currentIndex + 1
            }.toInt()

            try {
                Collections.swap(feeds, currentIndex, newPosition)

                val updates = feeds.mapIndexed { index, feed ->
                    FeedSavedSearchUpdate(
                        id = feed.id,
                        feedOrder = index.toLong(),
                    )
                }

                feedSavedSearchRepository.updatePartial(updates)
                Result.Success
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                Result.InternalError(e)
            }
        }
    }

    suspend fun sortAlphabetically(updates: List<FeedSavedSearchUpdate>?) = withNonCancellableContext {
        if (updates == null) return@withNonCancellableContext
        mutex.withLock {
            try {
                feedSavedSearchRepository.updatePartial(updates)
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

    private enum class MoveTo {
        UP,
        DOWN,
    }
}
