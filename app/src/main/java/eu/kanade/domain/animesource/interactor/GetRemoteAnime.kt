package eu.kanade.domain.animesource.interactor

import eu.kanade.domain.animesource.model.AnimeSourcePagingSourceType
import eu.kanade.domain.animesource.repository.AnimeSourceRepository
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList

class GetRemoteAnime(
    private val repository: AnimeSourceRepository,
) {

    fun subscribe(sourceId: Long, query: String, filterList: AnimeFilterList): AnimeSourcePagingSourceType {
        return when (query) {
            QUERY_POPULAR -> repository.getPopular(sourceId)
            QUERY_LATEST -> repository.getLatest(sourceId)
            else -> repository.search(sourceId, query, filterList)
        }
    }

    companion object {
        const val QUERY_POPULAR = "eu.kanade.domain.animesource.interactor.POPULAR"
        const val QUERY_LATEST = "eu.kanade.domain.animesource.interactor.LATEST"
    }
}
