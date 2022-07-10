package eu.kanade.domain.animetrack.repository

import eu.kanade.domain.animetrack.model.AnimeTrack
import kotlinx.coroutines.flow.Flow

interface AnimeTrackRepository {

    suspend fun getAnimeTracksByAnimeId(animeId: Long): List<AnimeTrack>

    fun getAnimeTracksAsFlow(): Flow<List<AnimeTrack>>

    fun getAnimeTracksByAnimeIdAsFlow(animeId: Long): Flow<List<AnimeTrack>>

    suspend fun delete(animeId: Long, syncId: Long)

    suspend fun insert(track: AnimeTrack)

    suspend fun insertAll(tracks: List<AnimeTrack>)
}
