package eu.kanade.domain.source.anime.interactor

import eu.kanade.domain.source.anime.model.AnimeSourcePagingSourceType
import eu.kanade.domain.source.anime.repository.AnimeSourceRepository
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList

class GetRemoteAnime(
    private val repository: AnimeSourceRepository,
) {

    fun subscribe(sourceId: Long, query: String, filterList: AnimeFilterList): AnimeSourcePagingSourceType {
        return when (query) {
            QUERY_POPULAR -> repository.getPopularAnime(sourceId)
            QUERY_LATEST -> repository.getLatestAnime(sourceId)
            else -> repository.searchAnime(sourceId, query, filterList)
        }
    }

    companion object {
        const val QUERY_POPULAR = "eu.kanade.domain.source.anime.interactor.POPULAR"
        const val QUERY_LATEST = "eu.kanade.domain.source.anime.interactor.LATEST"
    }
}
