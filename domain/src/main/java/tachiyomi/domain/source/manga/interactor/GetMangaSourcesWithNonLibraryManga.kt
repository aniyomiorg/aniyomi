package tachiyomi.domain.source.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.manga.model.MangaSourceWithCount
import tachiyomi.domain.source.manga.repository.MangaSourceRepository

class GetMangaSourcesWithNonLibraryManga(
    private val repository: MangaSourceRepository,
) {

    fun subscribe(): Flow<List<MangaSourceWithCount>> {
        return repository.getMangaSourcesWithNonLibraryManga()
    }
}
