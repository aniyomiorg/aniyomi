package tachiyomi.domain.track.manga.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.track.manga.model.MangaTrack
import tachiyomi.domain.track.manga.repository.MangaTrackRepository

class GetTracksPerManga(
    private val trackRepository: MangaTrackRepository,
) {

    fun subscribe(): Flow<Map<Long, List<MangaTrack>>> {
        return trackRepository.getMangaTracksAsFlow().map { tracks -> tracks.groupBy { it.mangaId } }
    }
}
