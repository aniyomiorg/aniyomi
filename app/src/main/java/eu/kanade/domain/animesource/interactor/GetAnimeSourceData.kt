package eu.kanade.domain.animesource.interactor

import eu.kanade.domain.animesource.model.AnimeSourceData
import eu.kanade.domain.animesource.repository.AnimeSourceRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class GetAnimeSourceData(
    private val repository: AnimeSourceRepository,
) {

    suspend fun await(id: Long): AnimeSourceData? {
        return try {
            repository.getAnimeSourceData(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }
}
