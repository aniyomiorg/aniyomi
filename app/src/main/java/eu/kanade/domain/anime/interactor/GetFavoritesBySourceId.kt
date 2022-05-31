package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.repository.AnimeRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesBySourceId(
    private val animeRepository: AnimeRepository,
) {

    fun subscribe(sourceId: Long): Flow<List<Anime>> {
        return animeRepository.getFavoritesBySourceId(sourceId)
    }
}
