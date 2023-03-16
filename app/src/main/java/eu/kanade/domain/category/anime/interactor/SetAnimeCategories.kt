package eu.kanade.domain.category.anime.interactor

import eu.kanade.domain.entries.anime.repository.AnimeRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

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
