package eu.kanade.tachiyomi.ui.browse.anime.source

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.source.anime.interactor.GetLanguagesWithAnimeSources
import eu.kanade.domain.source.anime.interactor.ToggleAnimeSource
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.source.service.ToggleLanguage
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.source.anime.model.AnimeSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeSourcesFilterScreenModel(
    private val preferences: SourcePreferences = Injekt.get(),
    private val getLanguagesWithSources: GetLanguagesWithAnimeSources = Injekt.get(),
    private val toggleSource: ToggleAnimeSource = Injekt.get(),
    private val toggleLanguage: ToggleLanguage = Injekt.get(),
) : StateScreenModel<AnimeSourcesFilterState>(AnimeSourcesFilterState.Loading) {

    init {
        coroutineScope.launch {
            combine(
                getLanguagesWithSources.subscribe(),
                preferences.enabledLanguages().changes(),
                preferences.disabledAnimeSources().changes(),
            ) { a, b, c -> Triple(a, b, c) }
                .catch { throwable ->
                    mutableState.update {
                        AnimeSourcesFilterState.Error(
                            throwable = throwable,
                        )
                    }
                }
                .collectLatest { (languagesWithSources, enabledLanguages, disabledSources) ->
                    mutableState.update {
                        AnimeSourcesFilterState.Success(
                            items = languagesWithSources,
                            enabledLanguages = enabledLanguages,
                            disabledSources = disabledSources,
                        )
                    }
                }
        }
    }

    fun toggleSource(source: AnimeSource) {
        toggleSource.await(source)
    }

    fun toggleLanguage(language: String) {
        toggleLanguage.await(language)
    }
}

sealed class AnimeSourcesFilterState {

    object Loading : AnimeSourcesFilterState()

    data class Error(
        val throwable: Throwable,
    ) : AnimeSourcesFilterState()

    data class Success(
        val items: Map<String, List<AnimeSource>>,
        val enabledLanguages: Set<String>,
        val disabledSources: Set<String>,
    ) : AnimeSourcesFilterState() {

        val isEmpty: Boolean
            get() = items.isEmpty()
    }
}
