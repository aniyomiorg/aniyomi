package tachiyomi.domain.track.manga.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.track.manga.model.MangaTrack
import tachiyomi.domain.track.manga.repository.MangaTrackRepository

class GetMangaTracks(
    private val trackRepository: MangaTrackRepository,
) {

    suspend fun awaitOne(id: Long): MangaTrack? {
        return try {
            trackRepository.getTrackByMangaId(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    // SY -->
    suspend fun await(): List<MangaTrack> {
        return try {
            trackRepository.getMangaTracks()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    suspend fun await(mangaIds: List<Long>): Map<Long, List<MangaTrack>> {
        return try {
            trackRepository.getTracksByMangaIds(mangaIds)
                .groupBy { it.mangaId }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyMap()
        }
    }
    // SY <--

    suspend fun await(mangaId: Long): List<MangaTrack> {
        return try {
            trackRepository.getTracksByMangaId(mangaId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    fun subscribe(mangaId: Long): Flow<List<MangaTrack>> {
        return trackRepository.getTracksByMangaIdAsFlow(mangaId)
    }
}
