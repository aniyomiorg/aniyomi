package eu.kanade.tachiyomi.ui.browse.animeextension

import android.os.Bundle
import eu.kanade.domain.animeextension.interactor.GetAnimeExtensionLanguages
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.animebrowse.AnimeExtensionFilterState
import eu.kanade.presentation.animebrowse.AnimeExtensionFilterStateImpl
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

class AnimeExtensionFilterPresenter(
    private val state: AnimeExtensionFilterStateImpl = AnimeExtensionFilterState() as AnimeExtensionFilterStateImpl,
    private val getExtensionLanguages: GetAnimeExtensionLanguages = Injekt.get(),
    private val toggleLanguage: ToggleLanguage = Injekt.get(),
    private val preferences: SourcePreferences = Injekt.get(),
) : BasePresenter<AnimeExtensionFilterController>(), AnimeExtensionFilterState by state {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        presenterScope.launchIO {
            getExtensionLanguages.subscribe()
                .catch { exception ->
                    logcat(LogPriority.ERROR, exception)
                    _events.send(Event.FailedFetchingLanguages)
                }
                .collectLatest(::collectLatestSourceLangMap)
        }
    }

    private fun collectLatestSourceLangMap(extLangs: List<String>) {
        val enabledLanguages = preferences.enabledLanguages().get()
        state.items = extLangs.map {
            AnimeFilterUiModel(it, it in enabledLanguages)
        }
        state.isLoading = false
    }

    fun toggleLanguage(language: String) {
        toggleLanguage.await(language)
    }

    sealed class Event {
        object FailedFetchingLanguages : Event()
    }
}
