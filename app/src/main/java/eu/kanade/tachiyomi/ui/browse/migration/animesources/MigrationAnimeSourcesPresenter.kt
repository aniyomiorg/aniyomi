package eu.kanade.tachiyomi.ui.browse.migration.animesources

import android.os.Bundle
import eu.kanade.domain.animesource.interactor.GetAnimeSourcesWithFavoriteCount
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrationAnimeSourcesPresenter(
    private val getSourcesWithFavoriteCount: GetAnimeSourcesWithFavoriteCount = Injekt.get(),
    private val setMigrateSorting: SetMigrateSorting = Injekt.get()
) : BasePresenter<MigrationAnimeSourcesController>() {

    private val _state: MutableStateFlow<MigrateAnimeSourceState> = MutableStateFlow(MigrateAnimeSourceState.Loading)
    val state: StateFlow<MigrateAnimeSourceState> = _state.asStateFlow()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        presenterScope.launchIO {
            getSourcesWithFavoriteCount.subscribe()
                .catch { exception ->
                    _state.value = MigrateAnimeSourceState.Error(exception)
                }
                .collectLatest { sources ->
                    _state.value = MigrateAnimeSourceState.Success(sources)
                }
        }
    }

    fun setAlphabeticalSorting(isAscending: Boolean) {
        setMigrateSorting.await(SetMigrateSorting.Mode.ALPHABETICAL, isAscending)
    }

    fun setTotalSorting(isAscending: Boolean) {
        setMigrateSorting.await(SetMigrateSorting.Mode.TOTAL, isAscending)
    }
}

sealed class MigrateAnimeSourceState {
    object Loading : MigrateAnimeSourceState()
    data class Error(val error: Throwable) : MigrateAnimeSourceState()
    data class Success(val sources: List<Pair<AnimeSource, Long>>) : MigrateAnimeSourceState()
}
