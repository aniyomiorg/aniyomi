package eu.kanade.tachiyomi.ui.browse.manga.source

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.source.manga.interactor.GetLanguagesWithMangaSources
import eu.kanade.domain.source.manga.interactor.ToggleMangaSource
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.source.service.ToggleLanguage
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.source.manga.model.Source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.SortedMap

class SourcesFilterScreenModel(
    private val preferences: SourcePreferences = Injekt.get(),
    private val getLanguagesWithSources: GetLanguagesWithMangaSources = Injekt.get(),
    private val toggleSource: ToggleMangaSource = Injekt.get(),
    private val toggleLanguage: ToggleLanguage = Injekt.get(),
) : StateScreenModel<SourcesFilterScreenModel.State>(State.Loading) {

    init {
        coroutineScope.launch {
            combine(
                getLanguagesWithSources.subscribe(),
                preferences.enabledLanguages().changes(),
                preferences.disabledMangaSources().changes(),
            ) { a, b, c -> Triple(a, b, c) }
                .catch { throwable ->
                    mutableState.update {
                        State.Error(
                            throwable = throwable,
                        )
                    }
                }
                .collectLatest { (languagesWithSources, enabledLanguages, disabledSources) ->
                    mutableState.update {
                        State.Success(
                            items = languagesWithSources,
                            enabledLanguages = enabledLanguages,
                            disabledSources = disabledSources,
                        )
                    }
                }
        }
    }

    fun toggleSource(source: Source) {
        toggleSource.await(source)
    }

    fun toggleLanguage(language: String) {
        toggleLanguage.await(language)
    }

    sealed interface State {

        @Immutable
        data object Loading : State

        @Immutable
        data class Error(
            val throwable: Throwable,
        ) : State

        @Immutable
        data class Success(
            val items: SortedMap<String, List<Source>>,
            val enabledLanguages: Set<String>,
            val disabledSources: Set<String>,
        ) : State {

            val isEmpty: Boolean
                get() = items.isEmpty()
        }
    }
}
