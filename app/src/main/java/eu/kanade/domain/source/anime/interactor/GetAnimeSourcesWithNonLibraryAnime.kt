package eu.kanade.domain.source.anime.interactor

import eu.kanade.domain.source.anime.repository.AnimeSourceRepository
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.anime.model.AnimeSourceWithCount

class GetAnimeSourcesWithNonLibraryAnime(
    private val repository: AnimeSourceRepository,
) {

    fun subscribe(): Flow<List<AnimeSourceWithCount>> {
        return repository.getSourcesWithNonLibraryAnime()
    }
}
