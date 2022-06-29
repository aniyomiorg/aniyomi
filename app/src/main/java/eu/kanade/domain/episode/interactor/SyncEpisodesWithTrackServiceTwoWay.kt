package eu.kanade.domain.episode.interactor

import eu.kanade.domain.animetrack.interactor.InsertAnimeTrack
import eu.kanade.domain.animetrack.model.AnimeTrack
import eu.kanade.domain.animetrack.model.toDbTrack
import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.model.toEpisodeUpdate
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SyncEpisodesWithTrackServiceTwoWay(
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val insertTrack: InsertAnimeTrack = Injekt.get(),
) {

    suspend fun await(
        episodes: List<Episode>,
        remoteTrack: AnimeTrack,
        service: TrackService,
    ) {
        val sortedEpisodes = episodes.sortedBy { it.episodeNumber }
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
