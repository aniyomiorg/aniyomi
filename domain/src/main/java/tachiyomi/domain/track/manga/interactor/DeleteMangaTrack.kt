package tachiyomi.domain.track.manga.interactor

import logcat.LogPriority
import tachiyomi.core.util.system.logcat
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
