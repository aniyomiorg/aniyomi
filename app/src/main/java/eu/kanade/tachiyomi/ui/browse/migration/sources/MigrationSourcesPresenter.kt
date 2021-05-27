package eu.kanade.tachiyomi.ui.browse.migration.sources

import android.os.Bundle
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrationSourcesPresenter(
    private val sourceManager: SourceManager = Injekt.get(),
    private val animesourceManager: AnimeSourceManager = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val animedb: AnimeDatabaseHelper = Injekt.get()
) : BasePresenter<MigrationSourcesController>() {

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        db.getFavoriteMangas()
            .asRxObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { findSourcesWithManga(it) }
            .subscribeLatestCache(MigrationSourcesController::setSources)
        animedb.getFavoriteAnimes()
            .asRxObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { findSourcesWithAnime(it) }
            .subscribeLatestCache(MigrationSourcesController::setAnimeSources)
    }

    private fun findSourcesWithManga(library: List<Manga>): List<SourceItem> {
        val header = SelectionHeader()
        return library
            .groupBy { it.source }
            .filterKeys { it != LocalSource.ID }
            .map {
                val source = sourceManager.getOrStub(it.key)
                SourceItem(source, it.value.size, header)
            }
            .sortedBy { it.source.name.toLowerCase() }
            .toList()
    }

    private fun findSourcesWithAnime(library: List<Anime>): List<AnimeSourceItem> {
        return library
            .groupBy { it.source }
            .filterKeys { it != LocalAnimeSource.ID }
            .map {
                val source = animesourceManager.getOrStub(it.key)
                AnimeSourceItem(source, it.value.size)
            }
            .sortedBy { it.source.name.toLowerCase() }
            .toList()
    }
}
