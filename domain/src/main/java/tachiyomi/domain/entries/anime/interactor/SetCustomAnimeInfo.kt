package tachiyomi.domain.entries.anime.interactor

import tachiyomi.domain.entries.anime.model.CustomAnimeInfo
import tachiyomi.domain.entries.anime.repository.CustomAnimeRepository

class SetCustomAnimeInfo(
    private val customAnimeRepository: CustomAnimeRepository,
) {

    fun set(animeInfo: CustomAnimeInfo) = customAnimeRepository.set(animeInfo)
}
