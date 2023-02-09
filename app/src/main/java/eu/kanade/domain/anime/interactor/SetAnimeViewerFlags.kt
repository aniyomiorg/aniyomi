package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.model.AnimeUpdate
import eu.kanade.domain.anime.repository.AnimeRepository

class SetAnimeViewerFlags(
    private val animeRepository: AnimeRepository,
) {

    suspend fun awaitSetSkipIntroLength(id: Long, skipIntroLength: Long) {
        // TODO: Convert to proper flag format
        // val anime = animeRepository.getAnimeById(id)
        animeRepository.update(
            AnimeUpdate(
                id = id,
                viewerFlags = skipIntroLength,
            ),
        )
    }
}
