package eu.kanade.domain.source.manga.interactor

import eu.kanade.domain.source.manga.model.MangaSourceWithCount
import eu.kanade.domain.source.manga.repository.MangaSourceRepository
import kotlinx.coroutines.flow.Flow

class GetMangaSourcesWithNonLibraryManga(
    private val repository: MangaSourceRepository,
) {

    fun subscribe(): Flow<List<MangaSourceWithCount>> {
        return repository.getMangaSourcesWithNonLibraryManga()
    }
}
