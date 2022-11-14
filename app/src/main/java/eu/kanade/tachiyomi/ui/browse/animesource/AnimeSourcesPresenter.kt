package eu.kanade.tachiyomi.ui.browse.animesource

import eu.kanade.domain.animesource.interactor.GetEnabledAnimeSources
import eu.kanade.domain.animesource.interactor.ToggleAnimeSource
import eu.kanade.domain.animesource.interactor.ToggleAnimeSourcePin
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.animesource.model.Pin
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.animebrowse.AnimeSourceUiModel
import eu.kanade.presentation.animebrowse.AnimeSourcesState
import eu.kanade.presentation.animebrowse.AnimeSourcesStateImpl
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.TreeMap

/**
 * Presenter of [AnimeSourcesController]
 * Function calls should be done from here. UI calls should be done from the controller.
 */
class AnimeSourcesPresenter(
    private val presenterScope: CoroutineScope,
    private val state: AnimeSourcesStateImpl = AnimeSourcesState() as AnimeSourcesStateImpl,
    private val preferences: BasePreferences = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val getEnabledAnimeSources: GetEnabledAnimeSources = Injekt.get(),
    private val toggleSource: ToggleAnimeSource = Injekt.get(),
    private val toggleSourcePin: ToggleAnimeSourcePin = Injekt.get(),
) : AnimeSourcesState by state {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    fun onCreate() {
        presenterScope.launchIO {
            getEnabledAnimeSources.subscribe()
                .catch { exception ->
                    logcat(LogPriority.ERROR, exception)
                    _events.send(Event.FailedFetchingSources)
                }
                .onStart { delay(500) } // Defer to avoid crashing on initial render
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
        state.isLoading = false
        state.items = uiModels
    }

    fun onOpenSource(source: AnimeSource) {
        if (!preferences.incognitoMode().get()) {
            sourcePreferences.lastUsedAnimeSource().set(source.id)
        }
    }

    fun toggleSource(source: AnimeSource) {
        toggleSource.await(source)
    }

    fun togglePin(source: AnimeSource) {
        toggleSourcePin.await(source)
    }

    sealed class Event {
        object FailedFetchingSources : Event()
    }

    data class Dialog(val source: AnimeSource)

    companion object {
        const val PINNED_KEY = "pinned"
        const val LAST_USED_KEY = "last_used"
    }
}
