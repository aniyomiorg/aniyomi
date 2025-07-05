package tachiyomi.domain.entries.manga.repository

import tachiyomi.domain.entries.manga.model.CustomMangaInfo

interface CustomMangaRepository {

    fun get(mangaId: Long): CustomMangaInfo?

    fun set(mangaInfo: CustomMangaInfo)
}
