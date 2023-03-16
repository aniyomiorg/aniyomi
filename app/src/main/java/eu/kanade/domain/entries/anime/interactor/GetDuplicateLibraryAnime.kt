package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.domain.entries.anime.repository.AnimeRepository

class GetDuplicateLibraryAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(title: String, sourceId: Long): Anime? {
        return animeRepository.getDuplicateLibraryAnime(title.lowercase(), sourceId)
    }
}
