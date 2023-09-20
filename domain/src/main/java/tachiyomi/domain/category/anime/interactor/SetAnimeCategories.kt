package tachiyomi.domain.category.anime.interactor

import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.anime.repository.AnimeRepository

class SetAnimeCategories(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(animeId: Long, categoryIds: List<Long>) {
        try {
            animeRepository.setAnimeCategories(animeId, categoryIds)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
