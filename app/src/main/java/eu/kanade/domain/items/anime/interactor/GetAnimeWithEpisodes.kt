package eu.kanade.domain.items.anime.interactor

import eu.kanade.domain.entries.episode.model.Episode
import eu.kanade.domain.entries.episode.repository.EpisodeRepository
import eu.kanade.domain.items.anime.model.Anime
import eu.kanade.domain.items.anime.repository.AnimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

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
