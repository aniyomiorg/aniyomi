package eu.kanade.domain.entries.manga.repository

import eu.kanade.domain.entries.manga.model.Manga
import eu.kanade.domain.entries.manga.model.MangaUpdate
import eu.kanade.domain.library.manga.LibraryManga
import kotlinx.coroutines.flow.Flow

interface MangaRepository {

    suspend fun getMangaById(id: Long): Manga

    suspend fun getMangaByIdAsFlow(id: Long): Flow<Manga>

    suspend fun getMangaByUrlAndSourceId(url: String, sourceId: Long): Manga?

    fun getMangaByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Manga?>

    suspend fun getMangaFavorites(): List<Manga>

    suspend fun getLibraryManga(): List<LibraryManga>

    fun getLibraryMangaAsFlow(): Flow<List<LibraryManga>>

    fun getMangaFavoritesBySourceId(sourceId: Long): Flow<List<Manga>>

    suspend fun getDuplicateLibraryManga(title: String, sourceId: Long): Manga?

    suspend fun resetMangaViewerFlags(): Boolean

    suspend fun setMangaCategories(mangaId: Long, categoryIds: List<Long>)

    suspend fun insertManga(manga: Manga): Long?

    suspend fun updateManga(update: MangaUpdate): Boolean

    suspend fun updateAllManga(mangaUpdates: List<MangaUpdate>): Boolean
}
