package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.entries.anime.model.Anime

class GetDuplicateLibraryAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(title: String): Anime? {
        return animeRepository.getDuplicateLibraryAnime(title.lowercase())
    }
}
