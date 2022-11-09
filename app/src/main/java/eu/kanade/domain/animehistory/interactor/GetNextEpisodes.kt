package eu.kanade.domain.animehistory.interactor

import eu.kanade.domain.anime.interactor.GetAnime
import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository
import eu.kanade.domain.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.episode.model.Episode
import eu.kanade.tachiyomi.util.episode.getEpisodeSort
import kotlin.math.max

class GetNextEpisodes(
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId,
    private val getAnime: GetAnime,
    private val historyRepository: AnimeHistoryRepository,
) {

    suspend fun await(onlyUnread: Boolean = true): List<Episode> {
        val history = historyRepository.getLastHistory() ?: return emptyList()
        return await(history.animeId, history.episodeId, onlyUnread)
    }

    suspend fun await(animeId: Long, onlyUnread: Boolean = true): List<Episode> {
        val anime = getAnime.await(animeId) ?: return emptyList()
        val episodes = getEpisodeByAnimeId.await(animeId)
            .sortedWith(getEpisodeSort(anime, sortDescending = false))

        return if (onlyUnread) {
            episodes.filterNot { it.seen }
        } else {
            episodes
        }
    }

    suspend fun await(animeId: Long, fromEpisodeId: Long, onlyUnread: Boolean = true): List<Episode> {
        val episodes = await(animeId, onlyUnread)
        val currEpisodeIndex = episodes.indexOfFirst { it.id == fromEpisodeId }
        val nextEpisodes = episodes.subList(max(0, currEpisodeIndex), episodes.size)

        if (onlyUnread) {
            return nextEpisodes
        }

        // The "next episode" is either:
        // - The current episode if it isn't completely seen
        // - The episodes after the current episode if the current one is completely seen
        val fromEpisode = episodes.getOrNull(currEpisodeIndex)
        return if (fromEpisode != null && !fromEpisode.seen) {
            nextEpisodes
        } else {
            nextEpisodes.drop(1)
        }
    }
}
