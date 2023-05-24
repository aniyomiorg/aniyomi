package tachiyomi.domain.entries.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUpdate
import tachiyomi.domain.library.anime.LibraryAnime

interface AnimeRepository {

    suspend fun getAnimeById(id: Long): Anime

    suspend fun getAnimeByIdAsFlow(id: Long): Flow<Anime>

    suspend fun getAnimeByUrlAndSourceId(url: String, sourceId: Long): Anime?

    fun getAnimeByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Anime?>

    suspend fun getAnimeFavorites(): List<Anime>

    suspend fun getLibraryAnime(): List<LibraryAnime>

    fun getLibraryAnimeAsFlow(): Flow<List<LibraryAnime>>

    fun getAnimeFavoritesBySourceId(sourceId: Long): Flow<List<Anime>>

    suspend fun getDuplicateLibraryAnime(title: String): Anime?

    suspend fun resetAnimeViewerFlags(): Boolean

    suspend fun setAnimeCategories(animeId: Long, categoryIds: List<Long>)

    suspend fun insertAnime(anime: Anime): Long?

    suspend fun updateAnime(update: AnimeUpdate): Boolean

    suspend fun updateAllAnime(animeUpdates: List<AnimeUpdate>): Boolean
}
