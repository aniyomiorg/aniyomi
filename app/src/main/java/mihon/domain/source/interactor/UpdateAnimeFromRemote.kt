package mihon.domain.source.interactor

import eu.kanade.domain.entries.anime.interactor.SyncSeasonsWithSource
import eu.kanade.domain.entries.anime.model.hasCustomBackground
import eu.kanade.domain.entries.anime.model.hasCustomCover
import eu.kanade.domain.entries.anime.model.toSAnime
import eu.kanade.domain.items.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.items.episode.model.toSEpisode
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.AnimeBackgroundCache
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import logcat.LogPriority
import mihon.domain.source.models.RemoteAnimeEpisodeUpdate
import mihon.domain.source.models.RemoteAnimeSeasonUpdate
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUpdate
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.repository.EpisodeRepository
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.source.local.entries.anime.isLocal
import java.time.Instant

class UpdateAnimeFromRemote(
    private val sourceManager: AnimeSourceManager,
    private val episodeRepository: EpisodeRepository,
    private val animeRepository: AnimeRepository,
    private val syncEpisodesWithSource: SyncEpisodesWithSource,
    private val syncSeasonsWithSource: SyncSeasonsWithSource,
    private val coverCache: AnimeCoverCache,
    private val backgroundCache: AnimeBackgroundCache,
) {
    suspend fun awaitEpisodesUpdate(
        anime: Anime,
        fetchDetails: Boolean = false,
        fetchEpisodes: Boolean = false,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): Result<RemoteAnimeEpisodeUpdate> {
        val source = sourceManager.getOrStub(anime.source)
        return awaitEpisodesUpdate(
            source = source,
            anime = anime,
            fetchDetails = fetchDetails,
            fetchEpisodes = fetchEpisodes,
            manualFetch = manualFetch,
            fetchWindow = fetchWindow,
        )
    }

    suspend fun awaitEpisodesUpdate(
        source: AnimeSource,
        anime: Anime,
        fetchDetails: Boolean = false,
        fetchEpisodes: Boolean = false,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): Result<RemoteAnimeEpisodeUpdate> {
        return try {
            val episodes = episodeRepository.getEpisodeByAnimeId(anime.id)
                .sortedBy { it.sourceOrder }
            val update = withIOContext {
                source.getAnimeEpisodeUpdate(
                    anime = anime.toSAnime(),
                    episodes = episodes.map(Episode::toSEpisode),
                    fetchDetails = fetchDetails,
                    fetchEpisodes = fetchEpisodes,
                )
            }
            awaitUpdateFromSource(anime, update.anime, manualFetch)
            val newEpisodes = syncEpisodesWithSource.await(
                rawSourceEpisodes = update.episodes,
                anime = anime,
                source = source,
                manualFetch = manualFetch,
                fetchWindow = fetchWindow,
            )
            val updatedAnime = animeRepository.getAnimeById(anime.id)
            Result.success(RemoteAnimeEpisodeUpdate(anime = updatedAnime, newEpisodes = newEpisodes))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.failure(e)
        }
    }

    suspend fun awaitSeasonsUpdate(
        anime: Anime,
        fetchDetails: Boolean = false,
        fetchSeasons: Boolean = false,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): Result<RemoteAnimeSeasonUpdate> {
        val source = sourceManager.getOrStub(anime.source)
        return awaitSeasonsUpdate(
            source = source,
            anime = anime,
            fetchDetails = fetchDetails,
            fetchSeasons = fetchSeasons,
            manualFetch = manualFetch,
            fetchWindow = fetchWindow,
        )
    }

    suspend fun awaitSeasonsUpdate(
        source: AnimeSource,
        anime: Anime,
        fetchDetails: Boolean = false,
        fetchSeasons: Boolean = false,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): Result<RemoteAnimeSeasonUpdate> {
        return try {
            val seasons = animeRepository.getAnimeSeasonsById(anime.id)
                .sortedBy { it.anime.seasonSourceOrder }
            val update = withIOContext {
                source.getAnimeSeasonUpdate(
                    anime = anime.toSAnime(),
                    seasons = seasons.map { it.anime.toSAnime() },
                    fetchDetails = fetchDetails,
                    fetchSeasons = fetchSeasons,
                )
            }
            awaitUpdateFromSource(anime, update.anime, manualFetch)
            val newSeasons = syncSeasonsWithSource.await(
                rawSourceSeasons = update.seasons,
                anime = anime,
                source = source,
                manualFetch = manualFetch,
                fetchWindow = fetchWindow,
            )
            val updatedAnime = animeRepository.getAnimeById(anime.id)
            Result.success(RemoteAnimeSeasonUpdate(anime = updatedAnime, newSeasons = newSeasons))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.failure(e)
        }
    }

    private suspend fun awaitUpdateFromSource(
        localAnime: Anime,
        remoteAnime: SAnime,
        manualFetch: Boolean,
    ): Boolean {
        val remoteTitle = try {
            remoteAnime.title
        } catch (_: UninitializedPropertyAccessException) {
            ""
        }

        // if the anime isn't a favorite, set its title from source and update in db
        val title =
            if (remoteTitle.isNotEmpty() && !localAnime.favorite) {
                remoteTitle
            } else {
                null
            }

        val coverLastModified =
            when {
                // Never refresh covers if the url is empty to avoid "losing" existing covers
                remoteAnime.thumbnail_url.isNullOrEmpty() -> null
                !manualFetch && localAnime.thumbnailUrl == remoteAnime.thumbnail_url -> null
                localAnime.isLocal() -> Instant.now().toEpochMilli()
                localAnime.hasCustomCover(coverCache) -> {
                    coverCache.deleteFromCache(localAnime, false)
                    null
                }
                else -> {
                    coverCache.deleteFromCache(localAnime, false)
                    Instant.now().toEpochMilli()
                }
            }

        val backgroundLastModified =
            when {
                // Never refresh backgrounds if the url is empty to avoid "losing" existing backgrounds
                remoteAnime.background_url.isNullOrEmpty() -> null
                !manualFetch && localAnime.backgroundUrl == remoteAnime.background_url -> null
                localAnime.isLocal() -> Instant.now().toEpochMilli()
                localAnime.hasCustomBackground(backgroundCache) -> {
                    backgroundCache.deleteFromCache(localAnime, false)
                    null
                }
                else -> {
                    backgroundCache.deleteFromCache(localAnime, false)
                    Instant.now().toEpochMilli()
                }
            }

        val thumbnailUrl = remoteAnime.thumbnail_url?.takeIf { it.isNotEmpty() }
        val backgroundUrl = remoteAnime.background_url?.takeIf { it.isNotEmpty() }

        return animeRepository.updateAnime(
            AnimeUpdate(
                id = localAnime.id,
                title = title,
                coverLastModified = coverLastModified,
                backgroundLastModified = backgroundLastModified,
                author = remoteAnime.author,
                artist = remoteAnime.artist,
                description = remoteAnime.description,
                genre = remoteAnime.getGenres(),
                thumbnailUrl = thumbnailUrl,
                backgroundUrl = backgroundUrl,
                status = remoteAnime.status.toLong(),
                updateStrategy = remoteAnime.update_strategy,
                initialized = true,
                memo = remoteAnime.memo,
            ),
        )
    }
}
