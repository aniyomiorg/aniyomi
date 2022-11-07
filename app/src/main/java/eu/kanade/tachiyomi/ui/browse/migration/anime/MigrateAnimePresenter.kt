package eu.kanade.tachiyomi.ui.browse.migration.anime

import android.os.Bundle
import eu.kanade.domain.anime.interactor.GetAnimeFavorites
import eu.kanade.presentation.animebrowse.MigrateAnimeState
import eu.kanade.presentation.animebrowse.MigrateAnimeStateImpl
import eu.kanade.presentation.animebrowse.MigrationAnimeState
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateAnimePresenter(
    private val sourceId: Long,
    private val state: MigrateAnimeStateImpl = MigrationAnimeState() as MigrateAnimeStateImpl,
    private val getFavorites: GetAnimeFavorites = Injekt.get(),
) : BasePresenter<MigrationAnimeController>(), MigrateAnimeState by state {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        presenterScope.launchIO {
            getFavorites
                .subscribe(sourceId)
                .catch { exception ->
                    logcat(LogPriority.ERROR, exception)
                    _events.send(Event.FailedFetchingFavorites)
                }
                .map { list ->
                    list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                }
                .collectLatest { sortedList ->
                    state.isLoading = false
                    state.items = sortedList
                }
        }
    }

    sealed class Event {
        object FailedFetchingFavorites : Event()
    }
}
