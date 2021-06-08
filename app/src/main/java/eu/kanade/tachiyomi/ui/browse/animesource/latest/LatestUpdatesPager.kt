package eu.kanade.tachiyomi.ui.browse.animesource.latest

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.ui.browse.animesource.browse.AnimePager
import eu.kanade.tachiyomi.util.lang.awaitSingle

class LatestUpdatesPager(val source: AnimeCatalogueSource) : AnimePager() {

    override suspend fun requestNextPage() {
        val animesPage = source.fetchLatestUpdates(currentPage).awaitSingle()
        onPageReceived(animesPage)
    }
}
