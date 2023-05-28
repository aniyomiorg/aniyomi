package tachiyomi.data.items.episode

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.util.lang.toLong
import tachiyomi.core.util.system.logcat
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.model.EpisodeUpdate
import tachiyomi.domain.items.episode.repository.EpisodeRepository

class EpisodeRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : EpisodeRepository {

    override suspend fun addAllEpisodes(episodes: List<Episode>): List<Episode> {
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

    override suspend fun updateEpisode(episodeUpdate: EpisodeUpdate) {
        partialUpdate(episodeUpdate)
    }

    override suspend fun updateAllEpisodes(episodeUpdates: List<EpisodeUpdate>) {
        partialUpdate(*episodeUpdates.toTypedArray())
    }

    private suspend fun partialUpdate(vararg episodeUpdates: EpisodeUpdate) {
        handler.await(inTransaction = true) {
            episodeUpdates.forEach { episodeUpdate ->
                episodesQueries.update(
                    animeId = episodeUpdate.animeId,
                    url = episodeUpdate.url,
                    name = episodeUpdate.name,
                    scanlator = episodeUpdate.scanlator,
                    seen = episodeUpdate.seen?.toLong(),
                    bookmark = episodeUpdate.bookmark?.toLong(),
                    lastSecondSeen = episodeUpdate.lastSecondSeen,
                    totalSeconds = episodeUpdate.totalSeconds,
                    episodeNumber = episodeUpdate.episodeNumber?.toDouble(),
                    sourceOrder = episodeUpdate.sourceOrder,
                    dateFetch = episodeUpdate.dateFetch,
                    dateUpload = episodeUpdate.dateUpload,
                    episodeId = episodeUpdate.id,
                )
            }
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

    override suspend fun getBookmarkedEpisodesByAnimeId(animeId: Long): List<Episode> {
        return handler.awaitList { episodesQueries.getBookmarkedEpisodesByAnimeId(animeId, episodeMapper) }
    }

    override suspend fun getEpisodeById(id: Long): Episode? {
        return handler.awaitOneOrNull { episodesQueries.getEpisodeById(id, episodeMapper) }
    }

    override suspend fun getEpisodeByAnimeIdAsFlow(animeId: Long): Flow<List<Episode>> {
        return handler.subscribeToList { episodesQueries.getEpisodesByAnimeId(animeId, episodeMapper) }
    }

    override suspend fun getEpisodeByUrlAndAnimeId(url: String, animeId: Long): Episode? {
        return handler.awaitOneOrNull { episodesQueries.getEpisodeByUrlAndAnimeId(url, animeId, episodeMapper) }
    }
}
