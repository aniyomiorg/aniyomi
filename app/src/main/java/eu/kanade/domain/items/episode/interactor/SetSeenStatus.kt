package eu.kanade.domain.items.episode.interactor

import eu.kanade.domain.download.anime.interactor.DeleteAnimeDownload
import logcat.LogPriority
import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.model.EpisodeUpdate
import tachiyomi.domain.items.episode.repository.EpisodeRepository

class SetSeenStatus(
    private val downloadPreferences: DownloadPreferences,
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

    suspend fun await(seen: Boolean, vararg episodes: Episode): Result = withNonCancellableContext {
        val episodesToUpdate = episodes.filter {
            when (seen) {
                true -> !it.seen
                false -> it.seen || it.lastSecondSeen > 0
            }
        }
        if (episodesToUpdate.isEmpty()) {
            return@withNonCancellableContext Result.NoEpisodes
        }

        try {
            episodeRepository.updateAllEpisodes(
                episodesToUpdate.map { mapper(it, seen) },
            )
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return@withNonCancellableContext Result.InternalError(e)
        }

        if (seen && downloadPreferences.removeAfterMarkedAsRead().get()) {
            episodesToUpdate
                .groupBy { it.animeId }
                .forEach { (animeId, episodes) ->
                    deleteDownload.awaitAll(
                        anime = animeRepository.getAnimeById(animeId),
                        episodes = episodes.toTypedArray(),
                    )
                }
        }

        Result.Success
    }

    suspend fun await(animeId: Long, seen: Boolean): Result = withNonCancellableContext {
        await(
            seen = seen,
            episodes = episodeRepository
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
