package tachiyomi.domain.track.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.track.manga.model.MangaTrack

interface MangaTrackRepository {

    suspend fun getTrackByMangaId(id: Long): MangaTrack?

    // SY -->
    suspend fun getMangaTracks(): List<MangaTrack>

    suspend fun getTracksByMangaIds(mangaIds: List<Long>): List<MangaTrack>
    // SY <--

    suspend fun getTracksByMangaId(mangaId: Long): List<MangaTrack>

    fun getMangaTracksAsFlow(): Flow<List<MangaTrack>>

    fun getTracksByMangaIdAsFlow(mangaId: Long): Flow<List<MangaTrack>>

    suspend fun deleteManga(mangaId: Long, syncId: Long)

    suspend fun insertManga(track: MangaTrack)

    suspend fun insertAllManga(tracks: List<MangaTrack>)
}
