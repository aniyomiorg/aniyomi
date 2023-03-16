package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.domain.entries.anime.repository.AnimeRepository
import kotlinx.coroutines.flow.Flow

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
