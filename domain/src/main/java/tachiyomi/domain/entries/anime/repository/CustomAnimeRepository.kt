package tachiyomi.domain.entries.anime.repository

import tachiyomi.domain.entries.anime.model.CustomAnimeInfo

interface CustomAnimeRepository {

    fun get(animeId: Long): CustomAnimeInfo?

    fun set(animeInfo: CustomAnimeInfo)
}
