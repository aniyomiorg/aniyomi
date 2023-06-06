package eu.kanade.domain.source.manga.interactor

import eu.kanade.domain.source.manga.repository.MangaSourceRepository
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.manga.model.MangaSourceWithCount

class GetMangaSourcesWithNonLibraryManga(
    private val repository: MangaSourceRepository,
) {

    fun subscribe(): Flow<List<MangaSourceWithCount>> {
        return repository.getMangaSourcesWithNonLibraryManga()
    }
}
