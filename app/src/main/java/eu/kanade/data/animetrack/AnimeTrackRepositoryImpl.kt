package eu.kanade.data.animetrack

import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.domain.animetrack.model.AnimeTrack
import eu.kanade.domain.animetrack.repository.AnimeTrackRepository
import kotlinx.coroutines.flow.Flow

class AnimeTrackRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeTrackRepository {

    override suspend fun getAnimeTracksByAnimeId(animeId: Long): List<AnimeTrack> {
        return handler.awaitList {
            anime_syncQueries.getTracksByAnimeId(animeId, animetrackMapper)
        }
    }

    override fun getAnimeTracksAsFlow(): Flow<List<AnimeTrack>> {
        return handler.subscribeToList {
            anime_syncQueries.getAnimeTracks(animetrackMapper)
        }
    }

    override fun getAnimeTracksByAnimeIdAsFlow(animeId: Long): Flow<List<AnimeTrack>> {
        return handler.subscribeToList {
            anime_syncQueries.getTracksByAnimeId(animeId, animetrackMapper)
        }
    }

    override suspend fun delete(animeId: Long, syncId: Long) {
        handler.await {
            anime_syncQueries.delete(
                animeId = animeId,
                syncId = syncId,
            )
        }
    }

    override suspend fun insert(track: AnimeTrack) {
        insertValues(track)
    }

    override suspend fun insertAll(tracks: List<AnimeTrack>) {
        insertValues(*tracks.toTypedArray())
    }

    private suspend fun insertValues(vararg tracks: AnimeTrack) {
        handler.await(inTransaction = true) {
            tracks.forEach { animeTrack ->
                anime_syncQueries.insert(
                    animeId = animeTrack.animeId,
                    syncId = animeTrack.syncId,
                    remoteId = animeTrack.remoteId,
                    libraryId = animeTrack.libraryId,
                    title = animeTrack.title,
                    lastEpisodeSeen = animeTrack.lastEpisodeSeen,
                    totalEpisodes = animeTrack.totalEpisodes,
                    status = animeTrack.status,
                    score = animeTrack.score,
                    remoteUrl = animeTrack.remoteUrl,
                    startDate = animeTrack.startDate,
                    finishDate = animeTrack.finishDate,
                )
            }
        }
    }
}
