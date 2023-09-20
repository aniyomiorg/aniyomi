package tachiyomi.data.entries.anime

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.util.lang.toLong
import tachiyomi.core.util.system.logcat
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.data.listOfStringsAdapter
import tachiyomi.data.updateStrategyAdapter
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUpdate
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.library.anime.LibraryAnime

class AnimeRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeRepository {

    override suspend fun getAnimeById(id: Long): Anime {
        return handler.awaitOne { animesQueries.getAnimeById(id, animeMapper) }
    }

    override suspend fun getAnimeByIdAsFlow(id: Long): Flow<Anime> {
        return handler.subscribeToOne { animesQueries.getAnimeById(id, animeMapper) }
    }

    override suspend fun getAnimeByUrlAndSourceId(url: String, sourceId: Long): Anime? {
        return handler.awaitOneOrNull(inTransaction = true) { animesQueries.getAnimeByUrlAndSource(url, sourceId, animeMapper) }
    }

    override fun getAnimeByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Anime?> {
        return handler.subscribeToOneOrNull { animesQueries.getAnimeByUrlAndSource(url, sourceId, animeMapper) }
    }

    override suspend fun getAnimeFavorites(): List<Anime> {
        return handler.awaitList { animesQueries.getFavorites(animeMapper) }
    }

    override suspend fun getLibraryAnime(): List<LibraryAnime> {
        return handler.awaitList { animelibViewQueries.animelib(libraryAnime) }
    }

    override fun getLibraryAnimeAsFlow(): Flow<List<LibraryAnime>> {
        return handler.subscribeToList { animelibViewQueries.animelib(libraryAnime) }
    }

    override fun getAnimeFavoritesBySourceId(sourceId: Long): Flow<List<Anime>> {
        return handler.subscribeToList { animesQueries.getFavoriteBySourceId(sourceId, animeMapper) }
    }

    override suspend fun getDuplicateLibraryAnime(title: String): Anime? {
        return handler.awaitOneOrNull {
            animesQueries.getDuplicateLibraryAnime(title, animeMapper)
        }
    }

    override suspend fun resetAnimeViewerFlags(): Boolean {
        return try {
            handler.await { animesQueries.resetViewerFlags() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun setAnimeCategories(animeId: Long, categoryIds: List<Long>) {
        handler.await(inTransaction = true) {
            animes_categoriesQueries.deleteAnimeCategoryByAnimeId(animeId)
            categoryIds.map { categoryId ->
                animes_categoriesQueries.insert(animeId, categoryId)
            }
        }
    }

    override suspend fun insertAnime(anime: Anime): Long? {
        return handler.awaitOneOrNull(inTransaction = true) {
            animesQueries.insert(
                source = anime.source,
                url = anime.url,
                artist = anime.artist,
                author = anime.author,
                description = anime.description,
                genre = anime.genre,
                title = anime.title,
                status = anime.status,
                thumbnailUrl = anime.thumbnailUrl,
                favorite = anime.favorite,
                lastUpdate = anime.lastUpdate,
                nextUpdate = anime.nextUpdate,
                calculateInterval = anime.calculateInterval.toLong(),
                initialized = anime.initialized,
                viewerFlags = anime.viewerFlags,
                episodeFlags = anime.episodeFlags,
                coverLastModified = anime.coverLastModified,
                dateAdded = anime.dateAdded,
                updateStrategy = anime.updateStrategy,
            )
            animesQueries.selectLastInsertedRowId()
        }
    }

    override suspend fun updateAnime(update: AnimeUpdate): Boolean {
        return try {
            partialUpdateAnime(update)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun updateAllAnime(animeUpdates: List<AnimeUpdate>): Boolean {
        return try {
            partialUpdateAnime(*animeUpdates.toTypedArray())
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    private suspend fun partialUpdateAnime(vararg animeUpdates: AnimeUpdate) {
        handler.await(inTransaction = true) {
            animeUpdates.forEach { value ->
                animesQueries.update(
                    source = value.source,
                    url = value.url,
                    artist = value.artist,
                    author = value.author,
                    description = value.description,
                    genre = value.genre?.let(listOfStringsAdapter::encode),
                    title = value.title,
                    status = value.status,
                    thumbnailUrl = value.thumbnailUrl,
                    favorite = value.favorite?.toLong(),
                    lastUpdate = value.lastUpdate,
                    nextUpdate = value.nextUpdate,
                    calculateInterval = value.calculateInterval?.toLong(),
                    initialized = value.initialized?.toLong(),
                    viewer = value.viewerFlags,
                    episodeFlags = value.episodeFlags,
                    coverLastModified = value.coverLastModified,
                    dateAdded = value.dateAdded,
                    animeId = value.id,
                    updateStrategy = value.updateStrategy?.let(updateStrategyAdapter::encode),
                )
            }
        }
    }
}
