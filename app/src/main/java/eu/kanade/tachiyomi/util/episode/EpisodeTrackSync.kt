package eu.kanade.tachiyomi.util.episode

import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.util.lang.launchIO
import timber.log.Timber

/**
 * Helper method for syncing a remote track with the local episodes, and back
 *
 * @param db the database.
 * @param episodes a list of episodes from the source.
 * @param remoteTrack the remote Track object.
 * @param service the tracker service.
 */
fun syncEpisodesWithTrackServiceTwoWay(db: AnimeDatabaseHelper, episodes: List<Episode>, remoteTrack: AnimeTrack, service: TrackService) {
    val sortedEpisodes = episodes.sortedBy { it.episode_number }
    sortedEpisodes
        .filter { episode -> episode.episode_number <= remoteTrack.last_episode_seen && !episode.seen }
        .forEach { it.seen = true }
    db.updateEpisodesProgress(sortedEpisodes).executeAsBlocking()

    // only take into account continuous reading
    val localLastSeen = sortedEpisodes.takeWhile { it.seen }.lastOrNull()?.episode_number ?: 0F

    // update remote
    remoteTrack.last_episode_seen = localLastSeen

    launchIO {
        try {
            service.update(remoteTrack)
            db.insertTrack(remoteTrack).executeAsBlocking()
        } catch (e: Throwable) {
            Timber.w(e)
        }
    }
}
