package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.repository.AnimeRepository
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority

class GetAnimeById(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(id: Long): Anime? {
        return try {
            animeRepository.getAnimeById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun subscribe(id: Long): Flow<Anime> {
        return animeRepository.subscribeAnimeById(id)
    }
}
