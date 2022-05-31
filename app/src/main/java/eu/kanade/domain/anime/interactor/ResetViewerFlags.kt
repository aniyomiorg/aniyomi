package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.repository.AnimeRepository

class ResetViewerFlags(
    private val animeRepository: AnimeRepository,
) {
    suspend fun await(): Boolean {
        return animeRepository.resetViewerFlags()
    }
}
