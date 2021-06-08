package eu.kanade.tachiyomi.ui.browse.animesource.browse

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.util.lang.awaitSingle

open class AnimeSourcePager(val source: AnimeCatalogueSource, val query: String, val filters: AnimeFilterList) : AnimePager() {

    override suspend fun requestNextPage() {
        val page = currentPage

        val observable = if (query.isBlank() && filters.isEmpty()) {
            source.fetchPopularAnime(page)
        } else {
            source.fetchSearchAnime(page, query, filters)
        }

        val animesPage = observable.awaitSingle()

        if (animesPage.animes.isNotEmpty()) {
            onPageReceived(animesPage)
        } else {
            throw NoResultsException()
        }
    }
}
