package eu.kanade.data.anime

import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.data.listOfStringsAdapter
import eu.kanade.data.toLong
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.AnimeUpdate
import eu.kanade.domain.anime.repository.AnimeRepository
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority

class AnimeRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeRepository {

    override suspend fun getAnimeById(id: Long): Anime {
        return handler.awaitOne { animesQueries.getAnimeById(id, animeMapper) }
    }

    override suspend fun subscribeAnimeById(id: Long): Flow<Anime> {
        return handler.subscribeToOne { animesQueries.getAnimeById(id, animeMapper) }
    }

    override suspend fun getAnimeByIdAsFlow(id: Long): Flow<Anime> {
        return handler.subscribeToOne { animesQueries.getAnimeById(id, animeMapper) }
    }

    override suspend fun getFavorites(): List<Anime> {
        return handler.awaitList { animesQueries.getFavorites(animeMapper) }
    }

    override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Anime>> {
        return handler.subscribeToList { animesQueries.getFavoriteBySourceId(sourceId, animeMapper) }
    }

    override suspend fun getDuplicateLibraryAnime(title: String, sourceId: Long): Anime? {
        return handler.awaitOneOrNull {
            animesQueries.getDuplicateLibraryAnime(title, sourceId, animeMapper)
        }
    }

    override suspend fun resetViewerFlags(): Boolean {
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

    override suspend fun update(update: AnimeUpdate): Boolean {
        return try {
            partialUpdate(update)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun updateAll(values: List<AnimeUpdate>): Boolean {
        return try {
            partialUpdate(*values.toTypedArray())
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    private suspend fun partialUpdate(vararg values: AnimeUpdate) {
        handler.await(inTransaction = true) {
            values.forEach { value ->
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
                    initialized = value.initialized?.toLong(),
                    viewer = value.viewerFlags,
                    episodeFlags = value.episodeFlags,
                    coverLastModified = value.coverLastModified,
                    dateAdded = value.dateAdded,
                    animeId = value.id,
                )
            }
        }
    }
}
