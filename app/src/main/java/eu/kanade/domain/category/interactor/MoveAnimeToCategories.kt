package eu.kanade.domain.category.interactor

import eu.kanade.domain.anime.repository.AnimeRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class MoveAnimeToCategories(
    private val mangaRepository: AnimeRepository,
) {

    suspend fun await(mangaId: Long, categoryIds: List<Long>) {
        try {
            mangaRepository.moveAnimeToCategories(mangaId, categoryIds)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
