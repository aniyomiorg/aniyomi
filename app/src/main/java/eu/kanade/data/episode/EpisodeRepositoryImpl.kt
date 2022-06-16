package eu.kanade.data.episode

import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.data.toLong
import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.model.EpisodeUpdate
import eu.kanade.domain.episode.repository.EpisodeRepository
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority

class EpisodeRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : EpisodeRepository {

    override suspend fun addAll(episodes: List<Episode>): List<Episode> {
        return try {
            handler.await(inTransaction = true) {
                episodes.map { episode ->
                    episodesQueries.insert(
                        episode.animeId,
                        episode.url,
                        episode.name,
                        episode.scanlator,
                        episode.seen,
                        episode.bookmark,
                        episode.lastSecondSeen,
                        episode.totalSeconds,
                        episode.episodeNumber,
                        episode.sourceOrder,
                        episode.dateFetch,
                        episode.dateUpload,
                    )
                    val lastInsertId = episodesQueries.selectLastInsertedRowId().executeAsOne()
                    episode.copy(id = lastInsertId)
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    override suspend fun update(episodeUpdate: EpisodeUpdate) {
        try {
            handler.await {
                episodesQueries.update(
                    episodeUpdate.animeId,
                    episodeUpdate.url,
                    episodeUpdate.name,
                    episodeUpdate.scanlator,
                    episodeUpdate.seen?.toLong(),
                    episodeUpdate.bookmark?.toLong(),
                    episodeUpdate.lastSecondSeen,
                    episodeUpdate.totalSeconds,
                    episodeUpdate.episodeNumber?.toDouble(),
                    episodeUpdate.sourceOrder,
                    episodeUpdate.dateFetch,
                    episodeUpdate.dateUpload,
                    episodeId = episodeUpdate.id,
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    override suspend fun updateAll(episodeUpdates: List<EpisodeUpdate>) {
        try {
            handler.await(inTransaction = true) {
                episodeUpdates.forEach { episodeUpdate ->
                    episodesQueries.update(
                        episodeUpdate.animeId,
                        episodeUpdate.url,
                        episodeUpdate.name,
                        episodeUpdate.scanlator,
                        episodeUpdate.seen?.toLong(),
                        episodeUpdate.bookmark?.toLong(),
                        episodeUpdate.lastSecondSeen,
                        episodeUpdate.totalSeconds,
                        episodeUpdate.episodeNumber?.toDouble(),
                        episodeUpdate.sourceOrder,
                        episodeUpdate.dateFetch,
                        episodeUpdate.dateUpload,
                        episodeId = episodeUpdate.id,
                    )
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    override suspend fun removeEpisodesWithIds(episodeIds: List<Long>) {
        try {
            handler.await { episodesQueries.removeEpisodesWithIds(episodeIds) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    override suspend fun getEpisodeByAnimeId(animeId: Long): List<Episode> {
        return handler.awaitList { episodesQueries.getEpisodesByAnimeId(animeId, episodeMapper) }
    }

    override fun getEpisodeByAnimeIdAsFlow(animeId: Long): Flow<List<Episode>> {
        return handler.subscribeToList { episodesQueries.getEpisodesByAnimeId(animeId, episodeMapper) }
    }
}
