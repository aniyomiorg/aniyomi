package eu.kanade.domain.episode.interactor

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.repository.AnimeRepository
import eu.kanade.domain.animedownload.interactor.DeleteAnimeDownload
import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.model.EpisodeUpdate
import eu.kanade.domain.episode.repository.EpisodeRepository
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import logcat.LogPriority

class SetSeenStatus(
    private val preferences: PreferencesHelper,
    private val deleteDownload: DeleteAnimeDownload,
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
) {

    private val mapper = { episode: Episode, read: Boolean ->
        EpisodeUpdate(
            seen = read,
            lastSecondSeen = if (!read) 0 else null,
            id = episode.id,
        )
    }

    suspend fun await(seen: Boolean, vararg values: Episode): Result = withContext(NonCancellable) f@{
        val episodes = values.filterNot { it.seen == seen }

        if (episodes.isEmpty()) {
            return@f Result.NoEpisodes
        }

        val anime = episodes.fold(mutableSetOf<Anime>()) { acc, episode ->
            if (acc.all { it.id != episode.animeId }) {
                acc += animeRepository.getAnimeById(episode.animeId)
            }
            acc
        }

        try {
            episodeRepository.updateAll(
                episodes.map { episode ->
                    mapper(episode, seen)
                },
            )
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return@f Result.InternalError(e)
        }

        if (seen && preferences.removeAfterMarkedAsRead()) {
            anime.forEach { anime ->
                deleteDownload.awaitAll(
                    anime = anime,
                    values = episodes
                        .filter { anime.id == it.animeId }
                        .toTypedArray(),
                )
            }
        }

        Result.Success
    }

    suspend fun await(animeId: Long, seen: Boolean): Result = withContext(NonCancellable) f@{
        return@f await(
            seen = seen,
            values = episodeRepository
                .getEpisodeByAnimeId(animeId)
                .toTypedArray(),
        )
    }

    suspend fun await(anime: Anime, seen: Boolean) =
        await(anime.id, seen)

    sealed class Result {
        object Success : Result()
        object NoEpisodes : Result()
        data class InternalError(val error: Throwable) : Result()
    }
}
