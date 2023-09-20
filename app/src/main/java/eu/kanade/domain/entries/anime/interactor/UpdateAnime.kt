package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.model.hasCustomCover
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUpdate
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.source.local.entries.anime.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

class UpdateAnime(
    private val animeRepository: AnimeRepository,
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
                localAnime.isLocal() -> Date().time
                localAnime.hasCustomCover(coverCache) -> {
                    coverCache.deleteFromCache(localAnime, false)
                    null
                }
                else -> {
                    coverCache.deleteFromCache(localAnime, false)
                    Date().time
                }
            }

        val thumbnailUrl = remoteAnime.thumbnail_url?.takeIf { it.isNotEmpty() }

        return animeRepository.updateAnime(
            AnimeUpdate(
                id = localAnime.id,
                title = title,
                coverLastModified = coverLastModified,
                author = remoteAnime.author,
                artist = remoteAnime.artist,
                description = remoteAnime.description,
                genre = remoteAnime.getGenres(),
                thumbnailUrl = thumbnailUrl,
                status = remoteAnime.status.toLong(),
                updateStrategy = remoteAnime.update_strategy,
                initialized = true,
            ),
        )
    }

    suspend fun awaitUpdateLastUpdate(animeId: Long): Boolean {
        return animeRepository.updateAnime(AnimeUpdate(id = animeId, lastUpdate = Date().time))
    }

    suspend fun awaitUpdateCoverLastModified(mangaId: Long): Boolean {
        return animeRepository.updateAnime(AnimeUpdate(id = mangaId, coverLastModified = Date().time))
    }

    suspend fun awaitUpdateFavorite(animeId: Long, favorite: Boolean): Boolean {
        val dateAdded = when (favorite) {
            true -> Date().time
            false -> 0
        }
        return animeRepository.updateAnime(
            AnimeUpdate(id = animeId, favorite = favorite, dateAdded = dateAdded),
        )
    }
}
