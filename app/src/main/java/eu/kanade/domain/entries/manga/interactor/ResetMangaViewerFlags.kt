package eu.kanade.domain.entries.manga.interactor

import eu.kanade.domain.entries.manga.repository.MangaRepository

class ResetMangaViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(): Boolean {
        return mangaRepository.resetMangaViewerFlags()
    }
}
