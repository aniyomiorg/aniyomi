package eu.kanade.domain.items.manga.interactor

import eu.kanade.domain.items.manga.model.Manga
import eu.kanade.domain.items.manga.repository.MangaRepository
import kotlinx.coroutines.flow.Flow

class GetMangaFavorites(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(): List<Manga> {
        return mangaRepository.getMangaFavorites()
    }

    fun subscribe(sourceId: Long): Flow<List<Manga>> {
        return mangaRepository.getMangaFavoritesBySourceId(sourceId)
    }
}
