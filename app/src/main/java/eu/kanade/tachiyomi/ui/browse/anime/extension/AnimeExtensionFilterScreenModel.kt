package eu.kanade.tachiyomi.ui.browse.anime.extension

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.extension.anime.interactor.GetAnimeExtensionLanguages
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.source.service.ToggleLanguage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeExtensionFilterScreenModel(
    private val preferences: SourcePreferences = Injekt.get(),
    private val getExtensionLanguages: GetAnimeExtensionLanguages = Injekt.get(),
    private val toggleLanguage: ToggleLanguage = Injekt.get(),
) : StateScreenModel<AnimeExtensionFilterState>(AnimeExtensionFilterState.Loading) {

    private val _events: Channel<AnimeExtensionFilterEvent> = Channel()
    val events: Flow<AnimeExtensionFilterEvent> = _events.receiveAsFlow()

    init {
        coroutineScope.launch {
            combine(
                getExtensionLanguages.subscribe(),
                preferences.enabledLanguages().changes(),
            ) { a, b -> a to b }
                .catch { throwable ->
                    logcat(LogPriority.ERROR, throwable)
                    _events.send(AnimeExtensionFilterEvent.FailedFetchingLanguages)
                }
                .collectLatest { (extensionLanguages, enabledLanguages) ->
                    mutableState.update {
                        AnimeExtensionFilterState.Success(
                            languages = extensionLanguages,
                            enabledLanguages = enabledLanguages,
                        )
                    }
                }
        }
    }

    fun toggle(language: String) {
        toggleLanguage.await(language)
    }
}

sealed class AnimeExtensionFilterEvent {
    object FailedFetchingLanguages : AnimeExtensionFilterEvent()
}

sealed class AnimeExtensionFilterState {

    @Immutable
    object Loading : AnimeExtensionFilterState()

    @Immutable
    data class Success(
        val languages: List<String>,
        val enabledLanguages: Set<String> = emptySet(),
    ) : AnimeExtensionFilterState() {

        val isEmpty: Boolean
            get() = languages.isEmpty()
    }
}
