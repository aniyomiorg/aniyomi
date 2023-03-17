package eu.kanade.domain.entries.manga.interactor

import eu.kanade.domain.entries.manga.model.Manga
import eu.kanade.domain.entries.manga.repository.MangaRepository

class GetDuplicateLibraryManga(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(title: String, sourceId: Long): Manga? {
        return mangaRepository.getDuplicateLibraryManga(title.lowercase(), sourceId)
    }
}
