package eu.kanade.tachiyomi.ui.browse.animesource.latest

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.ui.browse.animesource.browse.AnimePager
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourcePresenter

class LatestUpdatesPresenter(sourceId: Long) : BrowseAnimeSourcePresenter(sourceId) {

    override fun createPager(query: String, filters: AnimeFilterList): AnimePager {
        return LatestUpdatesPager(source)
    }
}
