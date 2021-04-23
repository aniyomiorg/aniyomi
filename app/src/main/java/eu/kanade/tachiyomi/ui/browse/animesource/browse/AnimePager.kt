package eu.kanade.tachiyomi.ui.browse.animesource.browse

import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.source.model.AnimesPage
import eu.kanade.tachiyomi.source.model.SAnime
import rx.Observable

/**
 * A general pager for source requests (latest updates, popular, search)
 */
abstract class AnimePager(var currentPage: Int = 1) {

    var hasNextPage = true
        private set

    protected val results: PublishRelay<Pair<Int, List<SAnime>>> = PublishRelay.create()

    fun results(): Observable<Pair<Int, List<SAnime>>> {
        return results.asObservable()
    }

    abstract fun requestNext(): Observable<AnimesPage>

    fun onPageReceived(animesPage: AnimesPage) {
        val page = currentPage
        currentPage++
        hasNextPage = animesPage.hasNextPage && animesPage.animes.isNotEmpty()
        results.call(Pair(page, animesPage.animes))
    }
}
