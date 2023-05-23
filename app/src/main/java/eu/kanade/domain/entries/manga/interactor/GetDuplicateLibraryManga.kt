package eu.kanade.domain.entries.manga.interactor

import eu.kanade.domain.entries.manga.repository.MangaRepository
import tachiyomi.domain.entries.manga.model.Manga

class GetDuplicateLibraryManga(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(title: String): Manga? {
        return mangaRepository.getDuplicateLibraryManga(title.lowercase())
    }
}
