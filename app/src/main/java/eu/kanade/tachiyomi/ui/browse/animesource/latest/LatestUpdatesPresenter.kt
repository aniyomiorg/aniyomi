package eu.kanade.tachiyomi.ui.browse.animesource.latest

import eu.kanade.tachiyomi.animesource.model.FilterList
import eu.kanade.tachiyomi.ui.browse.animesource.browse.AnimePager
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourcePresenter

/**
 * Presenter of [LatestUpdatesController]. Inherit BrowseCataloguePresenter.
 */
class LatestUpdatesPresenter(sourceId: Long) : BrowseAnimeSourcePresenter(sourceId) {

    override fun createPager(query: String, filters: FilterList): AnimePager {
        return LatestUpdatesPager(source)
    }
}
