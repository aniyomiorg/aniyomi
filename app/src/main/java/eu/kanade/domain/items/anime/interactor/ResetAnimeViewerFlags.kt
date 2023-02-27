package eu.kanade.domain.items.anime.interactor

import eu.kanade.domain.items.anime.repository.AnimeRepository

class ResetAnimeViewerFlags(
    private val animeRepository: AnimeRepository,
) {
    suspend fun await(): Boolean {
        return animeRepository.resetAnimeViewerFlags()
    }
}
