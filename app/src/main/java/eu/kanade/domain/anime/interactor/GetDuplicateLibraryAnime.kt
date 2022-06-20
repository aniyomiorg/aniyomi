package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.repository.AnimeRepository

class GetDuplicateLibraryAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(title: String, sourceId: Long): Anime? {
        return animeRepository.getDuplicateLibraryAnime(title.lowercase(), sourceId)
    }
}
