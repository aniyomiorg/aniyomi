package eu.kanade.tachiyomi.ui.browse.animesource.latest

import android.os.Bundle
import android.view.Menu
import androidx.core.os.bundleOf
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourceController
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourcePresenter

/**
 * Controller that shows the latest anime from the catalogue. Inherit [BrowseAnimeSourceController].
 */
class LatestUpdatesController(bundle: Bundle) : BrowseAnimeSourceController(bundle) {

    constructor(source: AnimeSource) : this(
        bundleOf(SOURCE_ID_KEY to source.id),
    )

    override fun createPresenter(): BrowseAnimeSourcePresenter {
        return LatestUpdatesPresenter(args.getLong(SOURCE_ID_KEY))
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_search).isVisible = false
    }

    override fun initFilterSheet() {
        // No-op: we don't allow filtering in latest
    }
}
