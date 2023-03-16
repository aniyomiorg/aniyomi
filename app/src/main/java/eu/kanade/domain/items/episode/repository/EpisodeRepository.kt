package eu.kanade.domain.items.episode.repository

import eu.kanade.domain.items.episode.model.Episode
import eu.kanade.domain.items.episode.model.EpisodeUpdate
import kotlinx.coroutines.flow.Flow

interface EpisodeRepository {

    suspend fun addAllEpisodes(episodes: List<Episode>): List<Episode>

    suspend fun updateEpisode(episodeUpdate: EpisodeUpdate)

    suspend fun updateAllEpisodes(episodeUpdates: List<EpisodeUpdate>)

    suspend fun removeEpisodesWithIds(episodeIds: List<Long>)

    suspend fun getEpisodeByAnimeId(animeId: Long): List<Episode>

    suspend fun getBookmarkedEpisodesByAnimeId(animeId: Long): List<Episode>

    suspend fun getEpisodeById(id: Long): Episode?

    suspend fun getEpisodeByAnimeIdAsFlow(animeId: Long): Flow<List<Episode>>

    suspend fun getEpisodeByUrlAndAnimeId(url: String, animeId: Long): Episode?
}
