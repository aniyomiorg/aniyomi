package tachiyomi.domain.entries.anime.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.repository.EpisodeRepository

class GetAnimeWithEpisodes(
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun subscribe(id: Long): Flow<Pair<Anime, List<Episode>>> {
        return combine(
            animeRepository.getAnimeByIdAsFlow(id),
            episodeRepository.getEpisodeByAnimeIdAsFlow(id),
        ) { anime, episodes ->
            Pair(anime, episodes)
        }
    }

    suspend fun awaitAnime(id: Long): Anime {
        return animeRepository.getAnimeById(id)
    }

    suspend fun awaitEpisodes(id: Long): List<Episode> {
        return episodeRepository.getEpisodeByAnimeId(id)
    }
}
