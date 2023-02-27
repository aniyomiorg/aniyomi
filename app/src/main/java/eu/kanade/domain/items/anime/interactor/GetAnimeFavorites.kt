package eu.kanade.domain.items.anime.interactor

import eu.kanade.domain.items.anime.model.Anime
import eu.kanade.domain.items.anime.repository.AnimeRepository
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
