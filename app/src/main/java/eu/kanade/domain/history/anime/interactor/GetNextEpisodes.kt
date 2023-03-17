package eu.kanade.domain.history.anime.interactor

import eu.kanade.domain.entries.anime.interactor.GetAnime
import eu.kanade.domain.history.anime.repository.AnimeHistoryRepository
import eu.kanade.domain.items.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.items.episode.model.Episode
import eu.kanade.tachiyomi.util.episode.getEpisodeSort
import kotlin.math.max

class GetNextEpisodes(
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId,
    private val getAnime: GetAnime,
    private val historyRepository: AnimeHistoryRepository,
) {

    suspend fun await(onlyUnseen: Boolean = true): List<Episode> {
        val history = historyRepository.getLastAnimeHistory() ?: return emptyList()
        return await(history.animeId, history.episodeId, onlyUnseen)
    }

    suspend fun await(animeId: Long, onlyUnseen: Boolean = true): List<Episode> {
        val anime = getAnime.await(animeId) ?: return emptyList()
        val episodes = getEpisodeByAnimeId.await(animeId)
            .sortedWith(getEpisodeSort(anime, sortDescending = false))

        return if (onlyUnseen) {
            episodes.filterNot { it.seen }
        } else {
            episodes
        }
    }

    suspend fun await(animeId: Long, fromEpisodeId: Long, onlyUnseen: Boolean = true): List<Episode> {
        val episodes = await(animeId, onlyUnseen)
        val currEpisodeIndex = episodes.indexOfFirst { it.id == fromEpisodeId }
        val nextEpisodes = episodes.subList(max(0, currEpisodeIndex), episodes.size)

        if (onlyUnseen) {
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
