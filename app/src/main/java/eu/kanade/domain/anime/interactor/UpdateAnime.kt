package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.AnimeUpdate
import eu.kanade.domain.anime.model.hasCustomCover
import eu.kanade.domain.anime.model.isLocal
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.anime.repository.AnimeRepository
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import tachiyomi.animesource.model.AnimeInfo
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

class UpdateAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(animeUpdate: AnimeUpdate): Boolean {
        return animeRepository.update(animeUpdate)
    }

    suspend fun awaitUpdateFromSource(
        localAnime: Anime,
        remoteAnime: AnimeInfo,
        manualFetch: Boolean,
        coverCache: AnimeCoverCache = Injekt.get(),
    ): Boolean {
        // if the anime isn't a favorite, set its title from source and update in db
        val title = if (!localAnime.favorite) remoteAnime.title else null

        // Never refresh covers if the url is empty to avoid "losing" existing covers
        val updateCover = remoteAnime.cover.isNotEmpty() && (manualFetch || localAnime.thumbnailUrl != remoteAnime.cover)
        val coverLastModified = if (updateCover) {
            when {
                localAnime.isLocal() -> Date().time
                localAnime.hasCustomCover(coverCache) -> {
                    coverCache.deleteFromCache(localAnime.toDbAnime(), false)
                    null
                }
                else -> {
                    coverCache.deleteFromCache(localAnime.toDbAnime(), false)
                    Date().time
                }
            }
        } else null

        return animeRepository.update(
            AnimeUpdate(
                id = localAnime.id,
                title = title?.takeIf { it.isNotEmpty() },
                coverLastModified = coverLastModified,
                author = remoteAnime.author,
                artist = remoteAnime.artist,
                description = remoteAnime.description,
                genre = remoteAnime.genres,
                thumbnailUrl = remoteAnime.cover.takeIf { it.isNotEmpty() },
                status = remoteAnime.status.toLong(),
                initialized = true,
            ),
        )
    }

    suspend fun awaitUpdateLastUpdate(animeId: Long): Boolean {
        return animeRepository.update(AnimeUpdate(id = animeId, lastUpdate = Date().time))
    }

    suspend fun awaitUpdateCoverLastModified(mangaId: Long): Boolean {
        return animeRepository.update(AnimeUpdate(id = mangaId, coverLastModified = Date().time))
    }

    suspend fun awaitUpdateFavorite(animeId: Long, favorite: Boolean): Boolean {
        val dateAdded = when (favorite) {
            true -> Date().time
            false -> 0
        }
        return animeRepository.update(
            AnimeUpdate(id = animeId, favorite = favorite, dateAdded = dateAdded),
        )
    }
}
