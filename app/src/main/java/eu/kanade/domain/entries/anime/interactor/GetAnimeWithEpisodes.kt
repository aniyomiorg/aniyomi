package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.domain.entries.anime.repository.AnimeRepository
import eu.kanade.domain.items.episode.model.Episode
import eu.kanade.domain.items.episode.repository.EpisodeRepository
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
