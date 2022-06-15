package eu.kanade.domain.animesource.interactor

import eu.kanade.domain.animesource.model.AnimeSourceData
import eu.kanade.domain.animesource.repository.AnimeSourceRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class UpsertAnimeSourceData(
    private val repository: AnimeSourceRepository,
) {

    suspend fun await(sourceData: AnimeSourceData) {
        try {
            repository.upsertAnimeSourceData(sourceData.id, sourceData.lang, sourceData.name)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
