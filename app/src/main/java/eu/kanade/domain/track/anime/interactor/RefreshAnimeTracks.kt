package eu.kanade.domain.track.anime.interactor

import eu.kanade.domain.items.episode.interactor.SyncEpisodeProgressWithTrack
import eu.kanade.domain.track.anime.model.toDbTrack
import eu.kanade.domain.track.anime.model.toDomainTrack
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import tachiyomi.domain.track.anime.interactor.InsertAnimeTrack

class RefreshAnimeTracks(
    private val getTracks: GetAnimeTracks,
    private val trackManager: TrackManager,
    private val insertTrack: InsertAnimeTrack,
    private val syncEpisodeProgressWithTrack: SyncEpisodeProgressWithTrack,
) {

    /**
     * Fetches updated tracking data from all logged in trackers.
     *
     * @return Failed updates.
     */
    suspend fun await(animeId: Long): List<Pair<TrackService?, Throwable>> {
        return supervisorScope {
            return@supervisorScope getTracks.await(animeId)
                .map { track ->
                    async {
                        val service = trackManager.getService(track.syncId)
                        return@async try {
                            if (service?.isLoggedIn == true) {
                                val updatedTrack = service.animeService.refresh(track.toDbTrack())
                                insertTrack.await(updatedTrack.toDomainTrack()!!)
                                syncEpisodeProgressWithTrack.await(animeId, track, service.animeService)
                            }
                            null
                        } catch (e: Throwable) {
                            service to e
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
    }
}
