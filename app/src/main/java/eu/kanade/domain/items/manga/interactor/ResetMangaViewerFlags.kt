package eu.kanade.domain.items.manga.interactor

import eu.kanade.domain.items.manga.repository.MangaRepository

class ResetMangaViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(): Boolean {
        return mangaRepository.resetMangaViewerFlags()
    }
}
