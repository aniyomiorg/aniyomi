package eu.kanade.domain.animesource.interactor

import eu.kanade.domain.animesource.repository.AnimeSourceRepository
import eu.kanade.tachiyomi.animesource.AnimeSource
import kotlinx.coroutines.flow.Flow

class GetAnimeSourcesWithNonLibraryAnime(
    private val repository: AnimeSourceRepository,
) {

    fun subscribe(): Flow<List<Pair<AnimeSource, Long>>> {
        return repository.getSourcesWithNonLibraryAnime()
    }
}
