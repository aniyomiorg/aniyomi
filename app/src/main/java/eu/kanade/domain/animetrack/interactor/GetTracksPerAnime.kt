package eu.kanade.domain.animetrack.interactor

import eu.kanade.domain.animetrack.repository.AnimeTrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetTracksPerAnime(
    private val trackRepository: AnimeTrackRepository,
) {

    fun subscribe(): Flow<Map<Long, List<Long>>> {
        return trackRepository.getAnimeTracksAsFlow().map { tracks ->
            tracks
                .groupBy { it.animeId }
                .mapValues { entry ->
                    entry.value.map { it.syncId }
                }
        }
    }
}
