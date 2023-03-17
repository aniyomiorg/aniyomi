package eu.kanade.domain.track.anime.interactor

import eu.kanade.domain.track.anime.model.AnimeTrack
import eu.kanade.domain.track.anime.repository.AnimeTrackRepository
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority

class GetAnimeTracks(
    private val animetrackRepository: AnimeTrackRepository,
) {

    suspend fun awaitOne(id: Long): AnimeTrack? {
        return try {
            animetrackRepository.getTrackByAnimeId(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun await(animeId: Long): List<AnimeTrack> {
        return try {
            animetrackRepository.getTracksByAnimeId(animeId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    fun subscribe(animeId: Long): Flow<List<AnimeTrack>> {
        return animetrackRepository.getTracksByAnimeIdAsFlow(animeId)
    }
}
