package eu.kanade.tachiyomi.ui.setting.database

import android.os.Bundle
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ClearDatabasePresenter : BasePresenter<ClearDatabaseController>() {

    private val animedb = Injekt.get<AnimeDatabaseHelper>()

    private val animesourceManager = Injekt.get<AnimeSourceManager>()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        getDatabaseAnimeSourcesObservable()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(ClearDatabaseController::setItemsAnime)
    }

    fun clearDatabaseForAnimeSourceIds(animeSources: List<Long>) {
        animedb.deleteAnimesNotInLibraryBySourceIds(animeSources).executeAsBlocking()
        animedb.deleteHistoryNoLastSeen().executeAsBlocking()
    }

    private fun getDatabaseAnimeSourcesObservable(): Observable<List<ClearDatabaseAnimeSourceItem>> {
        return animedb.getSourceIdsWithNonLibraryAnime().asRxObservable()
            .map { sourceCounts ->
                sourceCounts.map {
                    val sourceObj = animesourceManager.getOrStub(it.source)
                    ClearDatabaseAnimeSourceItem(sourceObj, it.count)
                }.sortedBy { it.source.name }
            }
    }
}
