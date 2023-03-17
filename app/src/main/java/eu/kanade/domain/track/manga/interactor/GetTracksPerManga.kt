package eu.kanade.domain.track.manga.interactor

import eu.kanade.domain.track.manga.repository.MangaTrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetTracksPerManga(
    private val trackRepository: MangaTrackRepository,
) {

    fun subscribe(): Flow<Map<Long, List<Long>>> {
        return trackRepository.getMangaTracksAsFlow().map { tracks ->
            tracks
                .groupBy { it.mangaId }
                .mapValues { entry ->
                    entry.value.map { it.syncId }
                }
        }
    }
}
