package eu.kanade.tachiyomi.ui.setting.database

import android.os.Bundle
import eu.kanade.domain.animesource.interactor.GetAnimeSourcesWithNonLibraryAnime
import eu.kanade.domain.source.interactor.GetSourcesWithNonLibraryManga
import eu.kanade.tachiyomi.Database
import eu.kanade.tachiyomi.mi.AnimeDatabase
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.flow.collectLatest
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ClearDatabasePresenter(
    private val database: Database = Injekt.get(),
    private val animedatabase: AnimeDatabase = Injekt.get(),
    private val getSourcesWithNonLibraryManga: GetSourcesWithNonLibraryManga = Injekt.get(),
    private val getAnimeSourcesWithNonLibraryAnime: GetAnimeSourcesWithNonLibraryAnime = Injekt.get(),
) : BasePresenter<ClearDatabaseController>() {

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        presenterScope.launchIO {
            getAnimeSourcesWithNonLibraryAnime.subscribe()
                .collectLatest { list ->
                    val items = list
                        .map { (source, count) -> ClearDatabaseAnimeSourceItem(source, count) }
                        .sortedBy { it.source.name }

                    withUIContext { view?.setItemsAnime(items) }
                }
        }
        presenterScope.launchIO {
            getSourcesWithNonLibraryManga.subscribe()
                .collectLatest { list ->
                    val items = list
                        .map { (source, count) -> ClearDatabaseSourceItem(source, count) }
                        .sortedBy { it.source.name }

                    withUIContext { view?.setItems(items) }
                }
        }
    }

    fun clearDatabaseForSourceIds(sources: List<Long>) {
        database.mangasQueries.deleteMangasNotInLibraryBySourceIds(sources)
        database.historyQueries.removeResettedHistory()
    }

    fun clearDatabaseForAnimeSourceIds(animeSources: List<Long>) {
        animedatabase.animesQueries.deleteAnimesNotInLibraryBySourceIds(animeSources)
        animedatabase.animehistoryQueries.removeResettedHistory()
    }
}
