package tachiyomi.domain.entries.manga.interactor

import tachiyomi.domain.entries.manga.model.MangaUpdate
import tachiyomi.domain.entries.manga.repository.MangaRepository

class UpdateMangaNotes(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(mangaId: Long, notes: String): Boolean {
        return mangaRepository.updateManga(
            MangaUpdate(
                id = mangaId,
                notes = notes,
            ),
        )
    }
}
