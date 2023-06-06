package tachiyomi.domain.entries.manga.interactor

import tachiyomi.domain.entries.manga.repository.MangaRepository

class ResetMangaViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(): Boolean {
        return mangaRepository.resetMangaViewerFlags()
    }
}
