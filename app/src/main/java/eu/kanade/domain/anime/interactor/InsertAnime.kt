package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.repository.AnimeRepository

class InsertAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(anime: Anime): Long? {
        return animeRepository.insert(anime)
    }
}
