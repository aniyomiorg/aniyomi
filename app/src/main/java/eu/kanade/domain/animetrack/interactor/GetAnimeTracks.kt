package eu.kanade.domain.animetrack.interactor

import eu.kanade.domain.animetrack.model.AnimeTrack
import eu.kanade.domain.animetrack.repository.AnimeTrackRepository
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority

class GetAnimeTracks(
    private val animetrackRepository: AnimeTrackRepository,
) {

    suspend fun await(animeId: Long): List<AnimeTrack> {
        return try {
            animetrackRepository.getAnimeTracksByAnimeId(animeId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    fun subscribe(animeId: Long): Flow<List<AnimeTrack>> {
        return animetrackRepository.getAnimeTracksByAnimeIdAsFlow(animeId)
    }
}
