package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.model.hasCustomBackground
import eu.kanade.domain.entries.anime.model.hasCustomCover
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.AnimeBackgroundCache
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import tachiyomi.domain.entries.anime.interactor.AnimeFetchInterval
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUpdate
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.source.local.entries.anime.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.ZonedDateTime

class UpdateAnime(
    private val animeRepository: AnimeRepository,
    private val animeFetchInterval: AnimeFetchInterval,
) {

    suspend fun await(animeUpdate: AnimeUpdate): Boolean {
        return animeRepository.updateAnime(animeUpdate)
    }

    suspend fun awaitAll(animeUpdates: List<AnimeUpdate>): Boolean {
        return animeRepository.updateAllAnime(animeUpdates)
    }

    suspend fun awaitUpdateFromSource(
        localAnime: Anime,
        remoteAnime: SAnime,
        manualFetch: Boolean,
        coverCache: AnimeCoverCache = Injekt.get(),
        backgroundCache: AnimeBackgroundCache = Injekt.get(),
    ): Boolean {
        val remoteTitle = try {
            remoteAnime.title
        } catch (_: UninitializedPropertyAccessException) {
            ""
        }

        // if the anime isn't a favorite, set its title from source and update in db
        val title = if (remoteTitle.isEmpty() || localAnime.favorite) null else remoteTitle

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
            ),
        )
    }

    suspend fun awaitUpdateFetchInterval(
        anime: Anime,
        dateTime: ZonedDateTime = ZonedDateTime.now(),
        window: Pair<Long, Long> = animeFetchInterval.getWindow(dateTime),
    ): Boolean {
        return animeRepository.updateAnime(
            animeFetchInterval.toAnimeUpdate(anime, dateTime, window),
        )
    }

    suspend fun awaitUpdateLastUpdate(animeId: Long): Boolean {
        return animeRepository.updateAnime(AnimeUpdate(id = animeId, lastUpdate = Instant.now().toEpochMilli()))
    }

    suspend fun awaitUpdateCoverLastModified(animeId: Long): Boolean {
        return animeRepository.updateAnime(
            AnimeUpdate(id = animeId, coverLastModified = Instant.now().toEpochMilli()),
        )
    }

    suspend fun awaitUpdateBackgroundLastModified(animeId: Long): Boolean {
        return animeRepository.updateAnime(
            AnimeUpdate(id = animeId, backgroundLastModified = Instant.now().toEpochMilli()),
        )
    }

    suspend fun awaitUpdateFavorite(animeId: Long, favorite: Boolean): Boolean {
        val dateAdded = when (favorite) {
            true -> Instant.now().toEpochMilli()
            false -> 0
        }
        return animeRepository.updateAnime(
            AnimeUpdate(id = animeId, favorite = favorite, dateAdded = dateAdded),
        )
    }
}
