package eu.kanade.domain.episode.repository

import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.model.EpisodeUpdate
import kotlinx.coroutines.flow.Flow

interface EpisodeRepository {

    suspend fun addAll(episodes: List<Episode>): List<Episode>

    suspend fun update(episodeUpdate: EpisodeUpdate)

    suspend fun updateAll(episodeUpdates: List<EpisodeUpdate>)

    suspend fun removeEpisodesWithIds(episodeIds: List<Long>)

    suspend fun getEpisodeByAnimeId(animeId: Long): List<Episode>

    fun getEpisodeByAnimeIdAsFlow(animeId: Long): Flow<List<Episode>>
}
