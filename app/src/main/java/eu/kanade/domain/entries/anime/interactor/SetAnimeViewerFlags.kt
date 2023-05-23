package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.entries.anime.model.AnimeUpdate

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
