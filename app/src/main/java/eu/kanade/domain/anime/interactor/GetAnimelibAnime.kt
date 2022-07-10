package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.repository.AnimeRepository
import eu.kanade.tachiyomi.data.database.models.AnimelibAnime
import kotlinx.coroutines.flow.Flow

class GetAnimelibAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(): List<AnimelibAnime> {
        return animeRepository.getAnimelibAnime()
    }

    fun subscribe(): Flow<List<AnimelibAnime>> {
        return animeRepository.getAnimelibAnimeAsFlow()
    }
}
