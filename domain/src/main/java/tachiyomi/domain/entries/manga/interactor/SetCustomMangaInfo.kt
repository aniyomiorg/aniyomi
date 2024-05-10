package tachiyomi.domain.entries.manga.interactor

import tachiyomi.domain.entries.manga.model.CustomMangaInfo
import tachiyomi.domain.entries.manga.repository.CustomMangaRepository

class SetCustomMangaInfo(
    private val customMangaRepository: CustomMangaRepository,
) {

    fun set(mangaInfo: CustomMangaInfo) = customMangaRepository.set(mangaInfo)
}
