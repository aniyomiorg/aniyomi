package eu.kanade.domain.track.anime.interactor

import eu.kanade.domain.track.anime.model.AnimeTrack
import eu.kanade.domain.track.anime.repository.AnimeTrackRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

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
