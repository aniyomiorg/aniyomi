package tachiyomi.domain.entries.anime.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRepository

class GetAnimeFavorites(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(): List<Anime> {
        return animeRepository.getAnimeFavorites()
    }

    fun subscribe(sourceId: Long): Flow<List<Anime>> {
        return animeRepository.getAnimeFavoritesBySourceId(sourceId)
    }
}
