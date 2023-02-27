package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.repository.AnimeRepository
import eu.kanade.domain.animelib.model.AnimelibAnime
import kotlinx.coroutines.flow.Flow

class GetAnimelibAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(): List<AnimelibAnime> {
        return animeRepository.getLibraryAnime()
    }

    fun subscribe(): Flow<List<AnimelibAnime>> {
        return animeRepository.getLibraryAnimeAsFlow()
    }
}
