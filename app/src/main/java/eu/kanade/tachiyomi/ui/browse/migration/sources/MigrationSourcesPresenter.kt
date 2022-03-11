package eu.kanade.tachiyomi.ui.browse.migration.sources

import android.os.Bundle
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.Collator
import java.util.Collections
import java.util.Locale

class MigrationSourcesPresenter(
    private val animesourceManager: AnimeSourceManager = Injekt.get(),
    private val animedb: AnimeDatabaseHelper = Injekt.get()
) : BasePresenter<MigrationSourcesController>() {

    private val preferences: PreferencesHelper by injectLazy()

    private val sortRelay = BehaviorRelay.create(Unit)

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        animedb.getFavoriteAnimes()
            .asRxObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { findSourcesWithAnime(it) }
            .subscribeLatestCache(MigrationSourcesController::setAnimeSources)
    }

    private fun findSourcesWithAnime(library: List<Anime>): List<AnimeSourceItem> {
        return library
            .groupBy { it.source }
            .filterKeys { it != LocalAnimeSource.ID }
            .map {
                val source = animesourceManager.getOrStub(it.key)
                AnimeSourceItem(source, it.value.size)
            }
            .sortedWith(sortFnAnime())
            .toList()
    }

    fun sortFnAnime(): java.util.Comparator<AnimeSourceItem> {
        val sort by lazy {
            preferences.migrationSortingMode().get()
        }
        val direction by lazy {
            preferences.migrationSortingDirection().get()
        }

        val locale = Locale.getDefault()
        val collator = Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
        val sortFn: (AnimeSourceItem, AnimeSourceItem) -> Int = { a, b ->
            when (sort) {
                MigrationSourcesController.SortSetting.ALPHABETICAL -> collator.compare(a.source.name.lowercase(locale), b.source.name.lowercase(locale))
                MigrationSourcesController.SortSetting.TOTAL -> a.animeCount.compareTo(b.animeCount)
            }
        }

        return when (direction) {
            MigrationSourcesController.DirectionSetting.ASCENDING -> Comparator(sortFn)
            MigrationSourcesController.DirectionSetting.DESCENDING -> Collections.reverseOrder(sortFn)
        }
    }

    fun requestSortUpdate() {
        sortRelay.call(Unit)
    }
}
