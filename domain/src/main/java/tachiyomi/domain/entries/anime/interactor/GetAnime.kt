package tachiyomi.domain.entries.anime.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRepository

class GetAnime(
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
        return animeRepository.getAnimeByIdAsFlow(id)
    }

    fun subscribe(url: String, sourceId: Long): Flow<Anime?> {
        return animeRepository.getAnimeByUrlAndSourceIdAsFlow(url, sourceId)
    }
}
