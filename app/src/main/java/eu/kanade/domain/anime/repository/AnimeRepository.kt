package eu.kanade.domain.anime.repository

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.AnimeUpdate
import kotlinx.coroutines.flow.Flow

interface AnimeRepository {

    suspend fun getAnimeById(id: Long): Anime

    suspend fun subscribeAnimeById(id: Long): Flow<Anime>

    suspend fun getAnimeByIdAsFlow(id: Long): Flow<Anime>

    suspend fun getFavorites(): List<Anime>

    fun getFavoritesBySourceId(sourceId: Long): Flow<List<Anime>>

    suspend fun getDuplicateLibraryAnime(title: String, sourceId: Long): Anime?

    suspend fun resetViewerFlags(): Boolean

    suspend fun setAnimeCategories(animeId: Long, categoryIds: List<Long>)

    suspend fun update(update: AnimeUpdate): Boolean

    suspend fun updateAll(values: List<AnimeUpdate>): Boolean
}
