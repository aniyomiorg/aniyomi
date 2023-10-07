package tachiyomi.domain.source.manga.interactor

import eu.kanade.tachiyomi.source.model.FilterList
import tachiyomi.domain.source.manga.repository.MangaSourceRepository
import tachiyomi.domain.source.manga.repository.SourcePagingSourceType

class GetRemoteManga(
    private val repository: MangaSourceRepository,
) {

    fun subscribe(sourceId: Long, query: String, filterList: FilterList): SourcePagingSourceType {
        return when (query) {
            QUERY_POPULAR -> repository.getPopularManga(sourceId)
            QUERY_LATEST -> repository.getLatestManga(sourceId)
            else -> repository.searchManga(sourceId, query, filterList)
        }
    }

    companion object {
        const val QUERY_POPULAR = "eu.kanade.domain.source.manga.interactor.POPULAR"
        const val QUERY_LATEST = "eu.kanade.domain.source.manga.interactor.LATEST"
    }
}
