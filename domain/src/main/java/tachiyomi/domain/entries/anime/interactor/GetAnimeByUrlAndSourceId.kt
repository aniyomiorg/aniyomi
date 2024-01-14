package tachiyomi.domain.entries.anime.interactor

import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRepository

class GetAnimeByUrlAndSourceId(
    private val animeRepository: AnimeRepository,
) {
    suspend fun await(url: String, sourceId: Long): Anime? {
        return animeRepository.getAnimeByUrlAndSourceId(url, sourceId)
    }
}
