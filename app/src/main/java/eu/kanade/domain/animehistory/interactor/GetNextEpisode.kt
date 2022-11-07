package eu.kanade.domain.animehistory.interactor

import eu.kanade.domain.anime.interactor.GetAnime
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository
import eu.kanade.domain.episode.interactor.GetEpisode
import eu.kanade.domain.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.episode.model.Episode
import eu.kanade.tachiyomi.util.episode.getEpisodeSort

class GetNextEpisode(
    private val getEpisode: GetEpisode,
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId,
    private val getAnime: GetAnime,
    private val historyRepository: AnimeHistoryRepository,
) {

    suspend fun await(): Episode? {
        val history = historyRepository.getLastHistory() ?: return null
        return await(history.animeId, history.episodeId)
    }

    suspend fun await(animeId: Long, episodeId: Long): Episode? {
        val episode = getEpisode.await(episodeId) ?: return null
        val anime = getAnime.await(animeId) ?: return null

        if (!episode.seen) return episode

        val episodes = getEpisodeByAnimeId.await(animeId)
            .sortedWith(getEpisodeSort(anime, sortDescending = false))

        val currEpisodeIndex = episodes.indexOfFirst { episode.id == it.id }
        return when (anime.sorting) {
            Anime.EPISODE_SORTING_SOURCE -> episodes.getOrNull(currEpisodeIndex + 1)
            Anime.EPISODE_SORTING_NUMBER -> {
                val episodeNumber = episode.episodeNumber

                ((currEpisodeIndex + 1) until episodes.size)
                    .map { episodes[it] }
                    .firstOrNull {
                        it.episodeNumber > episodeNumber && it.episodeNumber <= episodeNumber + 1
                    }
            }
            Anime.EPISODE_SORTING_UPLOAD_DATE -> {
                episodes.drop(currEpisodeIndex + 1)
                    .firstOrNull { it.dateUpload >= episode.dateUpload }
            }
            else -> throw NotImplementedError("Invalid episode sorting method: ${anime.sorting}")
        }
    }
}
