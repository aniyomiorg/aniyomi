package eu.kanade.domain.items.manga.interactor

import eu.kanade.domain.items.manga.model.Manga
import eu.kanade.domain.items.manga.repository.MangaRepository

class GetDuplicateLibraryManga(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(title: String, sourceId: Long): Manga? {
        return mangaRepository.getDuplicateLibraryManga(title.lowercase(), sourceId)
    }
}
