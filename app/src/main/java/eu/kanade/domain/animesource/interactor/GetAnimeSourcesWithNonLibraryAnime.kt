package eu.kanade.domain.animesource.interactor

import eu.kanade.domain.animesource.model.AnimeSourceWithCount
import eu.kanade.domain.animesource.repository.AnimeSourceRepository
import kotlinx.coroutines.flow.Flow

class GetAnimeSourcesWithNonLibraryAnime(
    private val repository: AnimeSourceRepository,
) {

    fun subscribe(): Flow<List<AnimeSourceWithCount>> {
        return repository.getSourcesWithNonLibraryAnime()
    }
}
