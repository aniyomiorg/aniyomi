package tachiyomi.domain.track.anime.interactor

import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.domain.track.anime.repository.AnimeTrackRepository

class InsertAnimeTrack(
    private val animetrackRepository: AnimeTrackRepository,
) {

    suspend fun await(track: AnimeTrack) {
        try {
            animetrackRepository.insertAnime(track)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitAll(tracks: List<AnimeTrack>) {
        try {
            animetrackRepository.insertAllAnime(tracks)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
