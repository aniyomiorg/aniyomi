package eu.kanade.domain.items.episode.interactor

import eu.kanade.domain.items.episode.model.Episode
import eu.kanade.domain.items.episode.model.toEpisodeUpdate
import eu.kanade.domain.track.anime.interactor.InsertAnimeTrack
import eu.kanade.domain.track.anime.model.AnimeTrack
import eu.kanade.domain.track.anime.model.toDbTrack
import eu.kanade.tachiyomi.data.track.AnimeTrackService
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
        service: AnimeTrackService,
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
