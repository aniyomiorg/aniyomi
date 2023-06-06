package tachiyomi.domain.track.manga.interactor

import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.track.manga.model.MangaTrack
import tachiyomi.domain.track.manga.repository.MangaTrackRepository

class InsertMangaTrack(
    private val trackRepository: MangaTrackRepository,
) {

    suspend fun await(track: MangaTrack) {
        try {
            trackRepository.insertManga(track)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitAll(tracks: List<MangaTrack>) {
        try {
            trackRepository.insertAllManga(tracks)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
