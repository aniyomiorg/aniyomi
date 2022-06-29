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

    override suspend fun subscribeAnimeTracksByAnimeId(animeId: Long): Flow<List<AnimeTrack>> {
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
        handler.await {
            anime_syncQueries.insert(
                animeId = track.animeId,
                syncId = track.syncId,
                remoteId = track.remoteId,
                libraryId = track.libraryId,
                title = track.title,
                lastEpisodeSeen = track.lastEpisodeSeen,
                totalEpisodes = track.totalEpisodes,
                status = track.status,
                score = track.score,
                remoteUrl = track.remoteUrl,
                startDate = track.startDate,
                finishDate = track.finishDate,
            )
        }
    }

    override suspend fun insertAll(tracks: List<AnimeTrack>) {
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
