package tachiyomi.domain.track.anime.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.track.anime.repository.AnimeTrackRepository

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
