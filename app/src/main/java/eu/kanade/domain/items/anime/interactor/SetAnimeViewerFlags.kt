package eu.kanade.domain.items.anime.interactor

import eu.kanade.domain.items.anime.model.AnimeUpdate
import eu.kanade.domain.items.anime.repository.AnimeRepository

class SetAnimeViewerFlags(
    private val animeRepository: AnimeRepository,
) {

    suspend fun awaitSetSkipIntroLength(id: Long, skipIntroLength: Long) {
        // TODO: Convert to proper flag format
        // val anime = animeRepository.getAnimeById(id)
        animeRepository.updateAnime(
            AnimeUpdate(
                id = id,
                viewerFlags = skipIntroLength,
            ),
        )
    }
}
