package eu.kanade.domain.track.anime.interactor

import eu.kanade.domain.track.anime.model.toDbTrack
import eu.kanade.tachiyomi.data.track.AnimeTracker
import eu.kanade.tachiyomi.data.track.EnhancedAnimeTracker
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.items.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.items.episode.interactor.UpdateEpisode
import tachiyomi.domain.items.episode.model.toEpisodeUpdate
import tachiyomi.domain.track.anime.interactor.InsertAnimeTrack
import tachiyomi.domain.track.anime.model.AnimeTrack

class SyncEpisodeProgressWithTrack(
    private val updateEpisode: UpdateEpisode,
    private val insertTrack: InsertAnimeTrack,
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
) {

    suspend fun await(
        animeId: Long,
        remoteTrack: AnimeTrack,
        service: AnimeTracker,
    ) {
        if (service !is EnhancedAnimeTracker) {
            return
        }

        val sortedEpisodes = getEpisodesByAnimeId.await(animeId)
            .sortedBy { it.episodeNumber }
            .filter { it.isRecognizedNumber }

        val episodeUpdates = sortedEpisodes
            .filter { episode -> episode.episodeNumber <= remoteTrack.lastEpisodeSeen && !episode.seen }
            .map { it.copy(seen = true).toEpisodeUpdate() }

        // only take into account continuous watching
        val localLastSeen = sortedEpisodes.takeWhile { it.seen }.lastOrNull()?.episodeNumber ?: 0F
        val updatedTrack = remoteTrack.copy(lastEpisodeSeen = localLastSeen.toDouble())

        try {
            service.update(updatedTrack.toDbTrack())
            updateEpisode.awaitAll(episodeUpdates)
            insertTrack.await(updatedTrack)
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e)
        }
    }
}
