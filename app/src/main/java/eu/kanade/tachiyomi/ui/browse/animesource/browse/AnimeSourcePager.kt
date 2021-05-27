package eu.kanade.tachiyomi.ui.browse.animesource.browse

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.FilterList
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

open class AnimeSourcePager(val source: AnimeCatalogueSource, val query: String, val filters: FilterList) : AnimePager() {

    override fun requestNext(): Observable<AnimesPage> {
        val page = currentPage

        val observable = if (query.isBlank() && filters.isEmpty()) {
            source.fetchPopularAnime(page)
        } else {
            source.fetchSearchAnime(page, query, filters)
        }

        return observable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                if (it.animes.isNotEmpty()) {
                    onPageReceived(it)
                } else {
                    throw NoResultsException()
                }
            }
    }
}
