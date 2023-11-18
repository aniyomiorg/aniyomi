package tachiyomi.domain.entries.anime.interactor

import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRepository

class GetDuplicateLibraryAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(anime: Anime): List<Anime> {
        return animeRepository.getDuplicateLibraryAnime(anime.id, anime.title.lowercase())
    }
}
