package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.repository.AnimeRepository

class ResetAnimeViewerFlags(
    private val animeRepository: AnimeRepository,
) {
    suspend fun await(): Boolean {
        return animeRepository.resetAnimeViewerFlags()
    }
}
