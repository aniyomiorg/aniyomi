package eu.kanade.data.animehistory

import androidx.paging.PagingSource
import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.data.anime.animeMapper
import eu.kanade.data.episode.episodeMapper
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.animehistory.model.AnimeHistoryUpdate
import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository
import eu.kanade.domain.episode.model.Episode
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class AnimeHistoryRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeHistoryRepository {

    override fun getHistory(query: String): PagingSource<Long, AnimeHistoryWithRelations> {
        return handler.subscribeToPagingSource(
            countQuery = { animehistoryViewQueries.countHistory(query) },
            transacter = { animehistoryViewQueries },
            queryProvider = { limit, offset ->
                animehistoryViewQueries.animehistory(query, limit, offset, animehistoryWithRelationsMapper)
            },
        )
    }

    override suspend fun getLastHistory(): AnimeHistoryWithRelations? {
        return handler.awaitOneOrNull {
            animehistoryViewQueries.getLatestAnimeHistory(animehistoryWithRelationsMapper)
        }
    }

    override suspend fun getNextEpisode(animeId: Long, episodeId: Long): Episode? {
        val episode = handler.awaitOne { episodesQueries.getEpisodeById(episodeId, episodeMapper) }
        val anime = handler.awaitOne { animesQueries.getAnimeById(animeId, animeMapper) }

        if (!episode.seen) {
            return episode
        }

        val sortFunction: (Episode, Episode) -> Int = when (anime.sorting) {
            Anime.EPISODE_SORTING_SOURCE -> { c1, c2 -> c2.sourceOrder.compareTo(c1.sourceOrder) }
            Anime.EPISODE_SORTING_NUMBER -> { c1, c2 -> c1.episodeNumber.compareTo(c2.episodeNumber) }
            Anime.EPISODE_SORTING_UPLOAD_DATE -> { c1, c2 -> c1.dateUpload.compareTo(c2.dateUpload) }
            else -> throw NotImplementedError("Unknown sorting method")
        }

        val episodes = handler.awaitList { episodesQueries.getEpisodesByAnimeId(animeId, episodeMapper) }
            .sortedWith(sortFunction)

        val currEpisodeIndex = episodes.indexOfFirst { episode.id == it.id }
        return when (anime.sorting) {
            Anime.EPISODE_SORTING_SOURCE -> episodes.getOrNull(currEpisodeIndex + 1)
            Anime.EPISODE_SORTING_NUMBER -> {
                val episodeNumber = episode.episodeNumber

                ((currEpisodeIndex + 1) until episodes.size)
                    .map { episodes[it] }
                    .firstOrNull {
                        it.episodeNumber > episodeNumber &&
                            it.episodeNumber <= episodeNumber + 1
                    }
            }
            Anime.EPISODE_SORTING_UPLOAD_DATE -> {
                episodes.drop(currEpisodeIndex + 1)
                    .firstOrNull { it.dateUpload >= episode.dateUpload }
            }
            else -> throw NotImplementedError("Unknown sorting method")
        }
    }

    override suspend fun resetHistory(historyId: Long) {
        try {
            handler.await { animehistoryQueries.resetAnimeHistoryById(historyId) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun resetHistoryByAnimeId(animeId: Long) {
        try {
            handler.await { animehistoryQueries.resetHistoryByAnimeId(animeId) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun deleteAllHistory(): Boolean {
        return try {
            handler.await { animehistoryQueries.removeAllHistory() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
            false
        }
    }

    override suspend fun upsertHistory(historyUpdate: AnimeHistoryUpdate) {
        try {
            handler.await {
                animehistoryQueries.upsert(
                    historyUpdate.episodeId,
                    historyUpdate.seenAt,
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }
}
