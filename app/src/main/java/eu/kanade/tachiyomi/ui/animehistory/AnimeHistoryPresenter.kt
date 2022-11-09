package eu.kanade.tachiyomi.ui.animehistory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.animehistory.interactor.GetAnimeHistory
import eu.kanade.domain.animehistory.interactor.GetNextEpisodes
import eu.kanade.domain.animehistory.interactor.RemoveAnimeHistory
import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.episode.model.Episode
import eu.kanade.presentation.animehistory.AnimeHistoryUiModel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.HistoryTabsController
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.toDateKey
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.preference.asState
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

class AnimeHistoryPresenter(
    private val presenterScope: CoroutineScope,
    val view: HistoryTabsController?,
    private val state: AnimeHistoryStateImpl = AnimeHistoryState() as AnimeHistoryStateImpl,
    private val getHistory: GetAnimeHistory = Injekt.get(),
    private val getNextEpisodes: GetNextEpisodes = Injekt.get(),
    private val removeHistory: RemoveAnimeHistory = Injekt.get(),
    preferences: BasePreferences = Injekt.get(),
) : AnimeHistoryState by state {

    private val _events: Channel<Event> = Channel(Int.MAX_VALUE)
    val events: Flow<Event> = _events.receiveAsFlow()

    val isDownloadOnly: Boolean by preferences.downloadedOnly().asState(presenterScope)
    val isIncognitoMode: Boolean by preferences.incognitoMode().asState(presenterScope)

    @Composable
    fun getHistory(): Flow<List<AnimeHistoryUiModel>> {
        val query = searchQuery ?: ""
        return remember(query) {
            getHistory.subscribe(query)
                .distinctUntilChanged()
                .catch { error ->
                    logcat(LogPriority.ERROR, error)
                    _events.send(Event.InternalError)
                }
                .map { pagingData ->
                    pagingData.toHistoryUiModels()
                }
        }
    }

    private fun List<AnimeHistoryWithRelations>.toHistoryUiModels(): List<AnimeHistoryUiModel> {
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

    fun getNextEpisodeForAnime(animeId: Long, episodeId: Long) {
        presenterScope.launchIO {
            sendNextEpisodeEvent(getNextEpisodes.await(animeId, episodeId, onlyUnread = false))
        }
    }

    fun resumeLastEpisodeSeen() {
        presenterScope.launchIO {
            sendNextEpisodeEvent(getNextEpisodes.await(onlyUnread = false))
        }
    }

    private suspend fun sendNextEpisodeEvent(episodes: List<Episode>) {
        val episode = episodes.firstOrNull()
        _events.send(if (episode != null) Event.OpenEpisode(episode) else Event.NoNextEpisodeFound)
    }

    fun removeFromHistory(history: AnimeHistoryWithRelations) {
        presenterScope.launchIO {
            removeHistory.await(history)
        }
    }

    fun removeAllFromHistory(animeId: Long) {
        presenterScope.launchIO {
            removeHistory.await(animeId)
        }
    }

    fun removeAllHistory() {
        presenterScope.launchIO {
            val result = removeHistory.awaitAll()
            if (!result) return@launchIO
            withUIContext {
                view?.activity?.toast(R.string.clear_history_completed)
            }
        }
    }

    sealed class Dialog {
        object DeleteAll : Dialog()
        data class Delete(val history: AnimeHistoryWithRelations) : Dialog()
    }

    sealed class Event {
        object InternalError : Event()
        object NoNextEpisodeFound : Event()
        data class OpenEpisode(val episode: Episode) : Event()
    }
}

@Stable
interface AnimeHistoryState {
    var searchQuery: String?
    var dialog: AnimeHistoryPresenter.Dialog?
}

fun AnimeHistoryState(): AnimeHistoryState {
    return AnimeHistoryStateImpl()
}

class AnimeHistoryStateImpl : AnimeHistoryState {
    override var searchQuery: String? by mutableStateOf(null)
    override var dialog: AnimeHistoryPresenter.Dialog? by mutableStateOf(null)
}
