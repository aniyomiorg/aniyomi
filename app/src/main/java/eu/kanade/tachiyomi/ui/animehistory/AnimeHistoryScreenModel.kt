package eu.kanade.tachiyomi.ui.animehistory

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.animehistory.interactor.GetAnimeHistory
import eu.kanade.domain.animehistory.interactor.GetNextEpisodes
import eu.kanade.domain.animehistory.interactor.RemoveAnimeHistory
import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import eu.kanade.domain.episode.model.Episode
import eu.kanade.presentation.animehistory.AnimeHistoryUiModel
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.toDateKey
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

class AnimeHistoryScreenModel(
    private val getHistory: GetAnimeHistory = Injekt.get(),
    private val getNextEpisodes: GetNextEpisodes = Injekt.get(),
    private val removeHistory: RemoveAnimeHistory = Injekt.get(),
) : StateScreenModel<AnimeHistoryState>(AnimeHistoryState()) {

    private val _events: Channel<Event> = Channel(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    init {
        coroutineScope.launch {
            state.map { it.searchQuery }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    getHistory.subscribe(query ?: "")
                        .distinctUntilChanged()
                        .catch { error ->
                            logcat(LogPriority.ERROR, error)
                            _events.send(Event.InternalError)
                        }
                        .map { it.toAnimeHistoryUiModels() }
                        .flowOn(Dispatchers.IO)
                }
                .collect { newList -> mutableState.update { it.copy(list = newList) } }
        }
    }

    private fun List<AnimeHistoryWithRelations>.toAnimeHistoryUiModels(): List<AnimeHistoryUiModel> {
        return map { AnimeHistoryUiModel.Item(it) }
            .insertSeparators { before, after ->
                val beforeDate = before?.item?.seenAt?.time?.toDateKey() ?: Date(0)
                val afterDate = after?.item?.seenAt?.time?.toDateKey() ?: Date(0)
                when {
                    beforeDate.time != afterDate.time && afterDate.time != 0L -> AnimeHistoryUiModel.Header(afterDate)
                    // Return null to avoid adding a separator between two items.
                    else -> null
                }
            }
    }

    suspend fun getNextEpisode(): Episode? {
        return withIOContext { getNextEpisodes.await(onlyUnseen = false).firstOrNull() }
    }

    fun getNextEpisodeForAnime(animeId: Long, episodeId: Long) {
        coroutineScope.launchIO {
            sendNextEpisodeEvent(getNextEpisodes.await(animeId, episodeId, onlyUnseen = false))
        }
    }

    private suspend fun sendNextEpisodeEvent(episodes: List<Episode>) {
        val episode = episodes.firstOrNull()
        _events.send(Event.OpenEpisode(episode))
    }

    fun removeFromHistory(history: AnimeHistoryWithRelations) {
        coroutineScope.launchIO {
            removeHistory.await(history)
        }
    }

    fun removeAllFromHistory(animeId: Long) {
        coroutineScope.launchIO {
            removeHistory.await(animeId)
        }
    }

    fun removeAllHistory() {
        coroutineScope.launchIO {
            val result = removeHistory.awaitAll()
            if (!result) return@launchIO
            _events.send(Event.HistoryCleared)
        }
    }

    val getSearchQuery = mutableState.value.searchQuery

    fun updateSearchQuery(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    sealed class Dialog {
        object DeleteAll : Dialog()
        data class Delete(val history: AnimeHistoryWithRelations) : Dialog()
    }

    sealed class Event {
        data class OpenEpisode(val episode: Episode?) : Event()
        object InternalError : Event()
        object HistoryCleared : Event()
    }
}

@Immutable
data class AnimeHistoryState(
    val searchQuery: String? = null,
    val list: List<AnimeHistoryUiModel>? = null,
    val dialog: AnimeHistoryScreenModel.Dialog? = null,
)
