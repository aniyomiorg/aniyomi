package eu.kanade.tachiyomi.ui.recent.animehistory

import android.os.Bundle
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import eu.kanade.domain.animehistory.interactor.DeleteAnimeHistoryTable
import eu.kanade.domain.animehistory.interactor.GetAnimeHistory
import eu.kanade.domain.animehistory.interactor.GetNextEpisode
import eu.kanade.domain.animehistory.interactor.RemoveAnimeHistoryByAnimeId
import eu.kanade.domain.animehistory.interactor.RemoveAnimeHistoryById
import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import eu.kanade.presentation.animehistory.HistoryUiModel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.toDateKey
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

/**
 * Presenter of HistoryFragment.
 * Contains information and data for fragment.
 * Observable updates should be called from here.
 */
class AnimeHistoryPresenter(
    private val getAnimeHistory: GetAnimeHistory = Injekt.get(),
    private val getNextEpisode: GetNextEpisode = Injekt.get(),
    private val deleteAnimeHistoryTable: DeleteAnimeHistoryTable = Injekt.get(),
    private val removeAnimeHistoryById: RemoveAnimeHistoryById = Injekt.get(),
    private val removeAnimeHistoryByAnimeId: RemoveAnimeHistoryByAnimeId = Injekt.get(),
) : BasePresenter<AnimeHistoryController>() {

    private val _query: MutableStateFlow<String> = MutableStateFlow("")
    private val _state: MutableStateFlow<HistoryState> = MutableStateFlow(HistoryState.Loading)
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        presenterScope.launchIO {
            _query.collectLatest { query ->
                getAnimeHistory.subscribe(query)
                    .catch { exception ->
                        _state.value = HistoryState.Error(exception)
                    }
                    .map { pagingData ->
                        pagingData.toAnimeHistoryUiModels()
                    }
                    .cachedIn(presenterScope)
                    .let { uiModelsPagingDataFlow ->
                        _state.value = HistoryState.Success(uiModelsPagingDataFlow)
                    }
            }
        }
    }

    private fun PagingData<AnimeHistoryWithRelations>.toAnimeHistoryUiModels(): PagingData<HistoryUiModel> {
        return this.map {
            HistoryUiModel.Item(it)
        }
            .insertSeparators { before, after ->
                val beforeDate = before?.item?.seenAt?.time?.toDateKey() ?: Date(0)
                val afterDate = after?.item?.seenAt?.time?.toDateKey() ?: Date(0)
                when {
                    beforeDate.time != afterDate.time && afterDate.time != 0L -> HistoryUiModel.Header(afterDate)
                    // Return null to avoid adding a separator between two items.
                    else -> null
                }
            }
    }

    fun search(query: String) {
        presenterScope.launchIO {
            _query.emit(query)
        }
    }

    fun removeFromHistory(history: AnimeHistoryWithRelations) {
        presenterScope.launchIO {
            removeAnimeHistoryById.await(history)
        }
    }

    fun removeAllFromHistory(animeId: Long) {
        presenterScope.launchIO {
            removeAnimeHistoryByAnimeId.await(animeId)
        }
    }

    fun getNextEpisodeForAnime(animeId: Long, episodeId: Long) {
        presenterScope.launchIO {
            val episode = getNextEpisode.await(animeId, episodeId)!!
            launchUI {
                view?.openEpisode(episode)
            }
        }
    }

    fun deleteAllHistory() {
        presenterScope.launchIO {
            val result = deleteAnimeHistoryTable.await()
            if (!result) return@launchIO
            launchUI {
                view?.activity?.toast(R.string.clear_history_completed)
            }
        }
    }

    fun resumeLastEpisodeSeen() {
        presenterScope.launchIO {
            val episode = getNextEpisode.await()
            launchUI {
                if (episode != null) {
                    view?.openEpisode(episode)
                }
            }
        }
    }
}

sealed class HistoryState {
    object Loading : HistoryState()
    data class Error(val error: Throwable) : HistoryState()
    data class Success(val uiModels: Flow<PagingData<HistoryUiModel>>) : HistoryState()
}
