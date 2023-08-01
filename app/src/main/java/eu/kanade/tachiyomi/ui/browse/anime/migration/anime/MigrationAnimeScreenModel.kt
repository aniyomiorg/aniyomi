package eu.kanade.tachiyomi.ui.browse.anime.migration.anime

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.tachiyomi.animesource.AnimeSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.anime.interactor.GetAnimeFavorites
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrationAnimeScreenModel(
    private val sourceId: Long,
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val getFavorites: GetAnimeFavorites = Injekt.get(),
) : StateScreenModel<MigrateAnimeState>(MigrateAnimeState()) {

    private val _events: Channel<MigrationAnimeEvent> = Channel()
    val events: Flow<MigrationAnimeEvent> = _events.receiveAsFlow()

    init {
        coroutineScope.launch {
            mutableState.update { state ->
                state.copy(source = sourceManager.getOrStub(sourceId))
            }

            getFavorites.subscribe(sourceId)
                .catch {
                    logcat(LogPriority.ERROR, it)
                    _events.send(MigrationAnimeEvent.FailedFetchingFavorites)
                    mutableState.update { state ->
                        state.copy(titleList = emptyList())
                    }
                }
                .map { anime ->
                    anime.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                }
                .collectLatest { list ->
                    mutableState.update { it.copy(titleList = list) }
                }
        }
    }
}

sealed class MigrationAnimeEvent {
    object FailedFetchingFavorites : MigrationAnimeEvent()
}

@Immutable
data class MigrateAnimeState(
    val source: AnimeSource? = null,
    private val titleList: List<Anime>? = null,
) {

    val titles: List<Anime>
        get() = titleList.orEmpty()

    val isLoading: Boolean
        get() = source == null || titleList == null

    val isEmpty: Boolean
        get() = titles.isEmpty()
}
