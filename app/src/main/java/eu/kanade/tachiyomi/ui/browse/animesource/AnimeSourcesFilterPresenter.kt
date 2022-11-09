package eu.kanade.tachiyomi.ui.browse.animesource

import android.os.Bundle
import eu.kanade.domain.animesource.interactor.GetLanguagesWithAnimeSources
import eu.kanade.domain.animesource.interactor.ToggleAnimeSource
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.animebrowse.AnimeSourcesFilterState
import eu.kanade.presentation.animebrowse.AnimeSourcesFilterStateImpl
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeSourcesFilterPresenter(
    private val state: AnimeSourcesFilterStateImpl = AnimeSourcesFilterState() as AnimeSourcesFilterStateImpl,
    private val getLanguagesWithSources: GetLanguagesWithAnimeSources = Injekt.get(),
    private val toggleSource: ToggleAnimeSource = Injekt.get(),
    private val toggleLanguage: ToggleLanguage = Injekt.get(),
    private val preferences: SourcePreferences = Injekt.get(),
) : BasePresenter<AnimeSourceFilterController>(), AnimeSourcesFilterState by state {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        presenterScope.launchIO {
            getLanguagesWithSources.subscribe()
                .catch { exception ->
                    logcat(LogPriority.ERROR, exception)
                    _events.send(Event.FailedFetchingLanguages)
                }
                .collectLatest(::collectLatestSourceLangMap)
        }
    }

    private fun collectLatestSourceLangMap(sourceLangMap: Map<String, List<AnimeSource>>) {
        state.items = sourceLangMap.flatMap {
            val isLangEnabled = it.key in preferences.enabledLanguages().get()
            val header = listOf(AnimeFilterUiModel.Header(it.key, isLangEnabled))

            if (isLangEnabled.not()) return@flatMap header
            header + it.value.map { source ->
                AnimeFilterUiModel.Item(
                    source,
                    source.id.toString() !in preferences.disabledSources().get(),
                )
            }
        }
        state.isLoading = false
    }

    private fun Map<String, List<AnimeSource>>.toFilterUiModels(): List<AnimeFilterUiModel> {
        return this.flatMap {
            val isLangEnabled = it.key in preferences.enabledLanguages().get()
            val header = listOf(AnimeFilterUiModel.Header(it.key, isLangEnabled))

            if (isLangEnabled.not()) return@flatMap header
            header + it.value.map { source ->
                AnimeFilterUiModel.Item(
                    source,
                    source.id.toString() !in preferences.disabledSources().get(),
                )
            }
        }
    }

    fun toggleSource(source: AnimeSource) {
        toggleSource.await(source)
    }

    fun toggleLanguage(language: String) {
        toggleLanguage.await(language)
    }
    sealed class Event {
        object FailedFetchingLanguages : Event()
    }
}
