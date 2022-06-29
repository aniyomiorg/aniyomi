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

    override suspend fun moveAnimeToCategories(animeId: Long, categoryIds: List<Long>) {
        handler.await(inTransaction = true) {
            animes_categoriesQueries.deleteAnimeCategoryByAnimeId(animeId)
            categoryIds.map { categoryId ->
                animes_categoriesQueries.insert(animeId, categoryId)
            }
        }
    }

    override suspend fun update(update: AnimeUpdate): Boolean {
        return try {
            handler.await {
                animesQueries.update(
                    source = update.source,
                    url = update.url,
                    artist = update.artist,
                    author = update.author,
                    description = update.description,
                    genre = update.genre?.let(listOfStringsAdapter::encode),
                    title = update.title,
                    status = update.status,
                    thumbnailUrl = update.thumbnailUrl,
                    favorite = update.favorite?.toLong(),
                    lastUpdate = update.lastUpdate,
                    initialized = update.initialized?.toLong(),
                    viewer = update.viewerFlags,
                    episodeFlags = update.episodeFlags,
                    coverLastModified = update.coverLastModified,
                    dateAdded = update.dateAdded,
                    animeId = update.id,
                )
            }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }
}
