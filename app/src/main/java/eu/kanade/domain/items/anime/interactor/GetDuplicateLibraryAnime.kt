package eu.kanade.domain.items.anime.interactor

import eu.kanade.domain.items.anime.model.Anime
import eu.kanade.domain.items.anime.repository.AnimeRepository

class GetDuplicateLibraryAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(title: String, sourceId: Long): Anime? {
        return animeRepository.getDuplicateLibraryAnime(title.lowercase(), sourceId)
    }
}
