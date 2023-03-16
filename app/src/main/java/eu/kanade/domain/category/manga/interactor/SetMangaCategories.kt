package eu.kanade.domain.category.manga.interactor

import eu.kanade.domain.entries.manga.repository.MangaRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class SetMangaCategories(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(mangaId: Long, categoryIds: List<Long>) {
        try {
            mangaRepository.setMangaCategories(mangaId, categoryIds)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
