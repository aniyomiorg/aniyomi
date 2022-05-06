package eu.kanade.tachiyomi.ui.browse.animesource

import android.os.Bundle
import eu.kanade.domain.animesource.interactor.GetEnabledAnimeSources
import eu.kanade.domain.animesource.interactor.ToggleAnimeSource
import eu.kanade.domain.animesource.interactor.ToggleAnimeSourcePin
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.animesource.model.Pin
import eu.kanade.presentation.animesource.AnimeSourceUiModel
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.TreeMap

/**
 * Presenter of [AnimeSourceController]
 * Function calls should be done from here. UI calls should be done from the controller.
 */
class AnimeSourcePresenter(
    private val getEnabledAnimeSources: GetEnabledAnimeSources = Injekt.get(),
    private val toggleSource: ToggleAnimeSource = Injekt.get(),
    private val toggleSourcePin: ToggleAnimeSourcePin = Injekt.get()
) : BasePresenter<AnimeSourceController>() {

    private val _state: MutableStateFlow<AnimeSourceState> = MutableStateFlow(AnimeSourceState.Loading)
    val state: StateFlow<AnimeSourceState> = _state.asStateFlow()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        presenterScope.launchIO {
            getEnabledAnimeSources.subscribe()
                .catch { exception ->
                    _state.value = AnimeSourceState.Error(exception)
                }
                .collectLatest(::collectLatestAnimeSources)
        }
    }

    private suspend fun collectLatestAnimeSources(sources: List<AnimeSource>) {
        val map = TreeMap<String, MutableList<AnimeSource>> { d1, d2 ->
            // Catalogues without a lang defined will be placed at the end
            when {
                d1 == LAST_USED_KEY && d2 != LAST_USED_KEY -> -1
                d2 == LAST_USED_KEY && d1 != LAST_USED_KEY -> 1
                d1 == PINNED_KEY && d2 != PINNED_KEY -> -1
                d2 == PINNED_KEY && d1 != PINNED_KEY -> 1
                d1 == "" && d2 != "" -> 1
                d2 == "" && d1 != "" -> -1
                else -> d1.compareTo(d2)
            }
        }
        val byLang = sources.groupByTo(map) {
            when {
                it.isUsedLast -> LAST_USED_KEY
                Pin.Actual in it.pin -> PINNED_KEY
                else -> it.lang
            }
        }

        val uiModels = byLang.flatMap {
            listOf(
                AnimeSourceUiModel.Header(it.key),
                *it.value.map { source ->
                    AnimeSourceUiModel.Item(source)
                }.toTypedArray(),
            )
        }
        _state.value = AnimeSourceState.Success(uiModels)
    }

    fun toggleSource(source: AnimeSource) {
        toggleSource.await(source)
    }

    fun togglePin(source: AnimeSource) {
        toggleSourcePin.await(source)
    }

    companion object {
        const val PINNED_KEY = "pinned"
        const val LAST_USED_KEY = "last_used"
    }
}

sealed class AnimeSourceState {
    object Loading : AnimeSourceState()
    data class Error(val error: Throwable) : AnimeSourceState()
    data class Success(val uiModels: List<AnimeSourceUiModel>) : AnimeSourceState()
}
