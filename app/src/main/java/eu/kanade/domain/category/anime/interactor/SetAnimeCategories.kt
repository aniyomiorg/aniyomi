package eu.kanade.domain.category.anime.interactor

import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority
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
