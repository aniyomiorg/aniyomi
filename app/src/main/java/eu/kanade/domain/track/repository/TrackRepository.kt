package eu.kanade.domain.track.repository

import eu.kanade.domain.track.model.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepository {

    suspend fun getTrackByMangaId(id: Long): Track?

    suspend fun getTracksByMangaId(mangaId: Long): List<Track>

    fun getMangaTracksAsFlow(): Flow<List<Track>>

    fun getTracksByMangaIdAsFlow(mangaId: Long): Flow<List<Track>>

    suspend fun deleteManga(mangaId: Long, syncId: Long)

    suspend fun insertManga(track: Track)

    suspend fun insertAllManga(tracks: List<Track>)
}
