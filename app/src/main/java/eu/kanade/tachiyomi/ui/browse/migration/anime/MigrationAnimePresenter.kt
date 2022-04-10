package eu.kanade.tachiyomi.ui.browse.migration.anime

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrationAnimePresenter(
    private val sourceId: Long,
    private val db: AnimeDatabaseHelper = Injekt.get(),
) : BasePresenter<MigrationAnimeController>() {

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        db.getFavoriteAnimes()
            .asRxObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { libraryToMigrationItem(it) }
            .subscribeLatestCache(MigrationAnimeController::setAnime)
    }

    private fun libraryToMigrationItem(library: List<Anime>): List<MigrationAnimeItem> {
        return library.filter { it.source == sourceId }
            .sortedBy { it.title }
            .map { MigrationAnimeItem(it) }
    }
}
