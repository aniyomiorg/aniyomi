package eu.kanade.domain.track.manga.interactor

import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority
import tachiyomi.domain.track.manga.repository.MangaTrackRepository

class DeleteMangaTrack(
    private val trackRepository: MangaTrackRepository,
) {

    suspend fun await(mangaId: Long, syncId: Long) {
        try {
            trackRepository.deleteManga(mangaId, syncId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
