package tachiyomi.domain.entries.anime.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.library.anime.LibraryAnime

class GetLibraryAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(): List<LibraryAnime> {
        return animeRepository.getLibraryAnime()
    }

    fun subscribe(): Flow<List<LibraryAnime>> {
        return animeRepository.getLibraryAnimeAsFlow()
    }
}
