package eu.kanade.domain.source.anime.interactor

import eu.kanade.domain.source.anime.model.AnimeSourceWithCount
import eu.kanade.domain.source.anime.repository.AnimeSourceRepository
import kotlinx.coroutines.flow.Flow

class GetAnimeSourcesWithNonLibraryAnime(
    private val repository: AnimeSourceRepository,
) {

    fun subscribe(): Flow<List<AnimeSourceWithCount>> {
        return repository.getSourcesWithNonLibraryAnime()
    }
}
