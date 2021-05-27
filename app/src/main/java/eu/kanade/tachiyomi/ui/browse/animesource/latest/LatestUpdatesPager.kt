package eu.kanade.tachiyomi.ui.browse.animesource.latest

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.ui.browse.animesource.browse.AnimePager
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * LatestUpdatesPager inherited from the general Pager.
 */
class LatestUpdatesPager(val source: AnimeCatalogueSource) : AnimePager() {

    override fun requestNext(): Observable<AnimesPage> {
        return source.fetchLatestUpdates(currentPage)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { onPageReceived(it) }
    }
}
