package tachiyomi.data.track.anime

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.domain.track.anime.repository.AnimeTrackRepository

class AnimeTrackRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeTrackRepository {

    override suspend fun getTrackByAnimeId(id: Long): AnimeTrack? {
        return handler.awaitOneOrNull { anime_syncQueries.getTrackByAnimeId(id, animeTrackMapper) }
    }

    override suspend fun getTracksByAnimeId(animeId: Long): List<AnimeTrack> {
        return handler.awaitList {
            anime_syncQueries.getTracksByAnimeId(animeId, animeTrackMapper)
        }
    }

    override fun getAnimeTracksAsFlow(): Flow<List<AnimeTrack>> {
        return handler.subscribeToList {
            anime_syncQueries.getAnimeTracks(animeTrackMapper)
        }
    }

    override fun getTracksByAnimeIdAsFlow(animeId: Long): Flow<List<AnimeTrack>> {
        return handler.subscribeToList {
            anime_syncQueries.getTracksByAnimeId(animeId, animeTrackMapper)
        }
    }

    override suspend fun deleteAnime(animeId: Long, syncId: Long) {
        handler.await {
            anime_syncQueries.delete(
                animeId = animeId,
                syncId = syncId,
            )
        }
    }

    override suspend fun insertAnime(track: AnimeTrack) {
        insertValues(track)
    }

    override suspend fun insertAllAnime(tracks: List<AnimeTrack>) {
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
