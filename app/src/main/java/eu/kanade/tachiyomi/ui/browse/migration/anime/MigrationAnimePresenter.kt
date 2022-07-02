package eu.kanade.tachiyomi.ui.browse.migration.anime

import android.os.Bundle
import eu.kanade.domain.anime.interactor.GetFavorites
import eu.kanade.domain.anime.model.Anime
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrationAnimePresenter(
    private val sourceId: Long,
    private val getFavorites: GetFavorites = Injekt.get(),
) : BasePresenter<MigrationAnimeController>() {

    private val _state: MutableStateFlow<MigrateAnimeState> = MutableStateFlow(MigrateAnimeState.Loading)
    val state: StateFlow<MigrateAnimeState> = _state.asStateFlow()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        presenterScope.launchIO {
            getFavorites
                .subscribe(sourceId)
                .catch { exception ->
                    _state.value = MigrateAnimeState.Error(exception)
                }
                .map { list ->
                    list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                }
                .collectLatest { sortedList ->
                    _state.value = MigrateAnimeState.Success(sortedList)
                }
        }
    }
}

sealed class MigrateAnimeState {
    object Loading : MigrateAnimeState()
    data class Error(val error: Throwable) : MigrateAnimeState()
    data class Success(val list: List<Anime>) : MigrateAnimeState()
}
