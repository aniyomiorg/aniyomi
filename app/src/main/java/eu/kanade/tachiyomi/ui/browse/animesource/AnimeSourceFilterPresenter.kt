package eu.kanade.tachiyomi.ui.browse.animesource

import android.os.Bundle
import eu.kanade.domain.animesource.interactor.GetLanguagesWithAnimeSources
import eu.kanade.domain.animesource.interactor.ToggleAnimeSource
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeSourceFilterPresenter(
    private val getLanguagesWithSources: GetLanguagesWithAnimeSources = Injekt.get(),
    private val toggleSource: ToggleAnimeSource = Injekt.get(),
    private val toggleLanguage: ToggleLanguage = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get()
) : BasePresenter<AnimeSourceFilterController>() {

    private val _state: MutableStateFlow<AnimeSourceFilterState> = MutableStateFlow(AnimeSourceFilterState.Loading)
    val state: StateFlow<AnimeSourceFilterState> = _state.asStateFlow()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        presenterScope.launchIO {
            getLanguagesWithSources.subscribe()
                .catch { exception ->
                    _state.value = AnimeSourceFilterState.Error(exception)
                }
                .collectLatest { sourceLangMap ->
                    val uiModels = sourceLangMap.toFilterUiModels()
                    _state.value = AnimeSourceFilterState.Success(uiModels)
                }
        }
    }

    private fun Map<String, List<AnimeSource>>.toFilterUiModels(): List<AnimeFilterUiModel> {
        return this.flatMap {
            val isLangEnabled = it.key in preferences.enabledLanguages().get()
            val header = listOf(AnimeFilterUiModel.Header(it.key, isLangEnabled))

            if (isLangEnabled.not()) return@flatMap header
            header + it.value.map { source ->
                AnimeFilterUiModel.Item(
                    source,
                    source.id.toString() !in preferences.disabledSources().get()
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
}

sealed class AnimeSourceFilterState {
    object Loading : AnimeSourceFilterState()
    data class Error(val error: Throwable) : AnimeSourceFilterState()
    data class Success(val models: List<AnimeFilterUiModel>) : AnimeSourceFilterState()
}
