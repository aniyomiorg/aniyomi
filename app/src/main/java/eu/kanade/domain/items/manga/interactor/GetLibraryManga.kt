package eu.kanade.domain.items.manga.interactor

import eu.kanade.domain.items.manga.repository.MangaRepository
import eu.kanade.domain.library.manga.LibraryManga
import kotlinx.coroutines.flow.Flow

class GetLibraryManga(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(): List<LibraryManga> {
        return mangaRepository.getLibraryManga()
    }

    fun subscribe(): Flow<List<LibraryManga>> {
        return mangaRepository.getLibraryMangaAsFlow()
    }
}
