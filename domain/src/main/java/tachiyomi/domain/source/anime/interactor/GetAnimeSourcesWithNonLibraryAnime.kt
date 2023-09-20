package tachiyomi.domain.source.anime.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.anime.model.AnimeSourceWithCount
import tachiyomi.domain.source.anime.repository.AnimeSourceRepository

class GetAnimeSourcesWithNonLibraryAnime(
    private val repository: AnimeSourceRepository,
) {

    fun subscribe(): Flow<List<AnimeSourceWithCount>> {
        return repository.getSourcesWithNonLibraryAnime()
    }
}
