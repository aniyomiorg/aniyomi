package eu.kanade.domain.track.manga.interactor

import eu.kanade.domain.track.manga.repository.MangaTrackRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

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
