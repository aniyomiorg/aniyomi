package eu.kanade.domain.track.manga.interactor

import eu.kanade.domain.track.manga.model.MangaTrack
import eu.kanade.domain.track.manga.repository.MangaTrackRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

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
