package tachiyomi.data.source.anime

import tachiyomi.domain.source.anime.model.FeedSavedSearch

object FeedSavedSearchMapper {
    fun map(
        id: Long,
        source: Long,
        savedSearch: Long?,
        global: Boolean,
        feedOrder: Long,
    ): FeedSavedSearch {
        return FeedSavedSearch(
            id = id,
            source = source,
            savedSearch = savedSearch,
            global = global,
            feedOrder = feedOrder,
        )
    }
}
